package app.paralino.android.domain

import app.paralino.android.AppClass
import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.history.UnconfirmedLocationHistoryEntity
import app.paralino.core.db.main.keys.PersonalLocalHistoryKeyEntity
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.LocationHistoryModelForUser
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.repo.user.ExportableLocationHistoryModelForUser
import app.paralino.core.repo.user.IImportPersonalLocationHistoryUseCase
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.extensions.nowUTC
import java.io.File


class ImportPersonalLocationHistoryUseCase(
    private val appClass: AppClass,
    private val cryptoCore: CryptoCore,
    private val cryptoUserKeysRepo: CryptoUserKeysLocalSource,
    private val database: MainDatabaseHelper,
    private val appPrefs: AppPrefs
): IImportPersonalLocationHistoryUseCase {

    companion object {
        private const val TAG = "import_location_history"
    }

    override suspend fun importPersonalLocationHistoryFromFile(
        fileName: String,
        locationId: String
    ): Result<Unit> {
        return try {
            val file = File(appClass.filesDir, fileName)
            logg.v(TAG, "Starting import of personal location history from ${file.absolutePath}")
            logg.v(TAG, "Using locationId: $locationId")

            // Read and parse JSON file
            logg.v(TAG, "Reading JSON file")
            val jsonContent = file.readText()
            logg.v(TAG, "File size: ${jsonContent.length} characters")

            val historyModel = BaseRemoteModule.formatter.decodeFromString<ExportableLocationHistoryModelForUser>(jsonContent)
            logg.v(TAG, "Parsed ${historyModel.history.size} history entries")

            val toImport = LocationHistoryModelForUser(
                groupId = historyModel.groupId,
                memberUserId = historyModel.memberUserId,
                history = historyModel.history.map {
                    LocationHistoryModelForDevice(
                        locationId = it.locationId,
                        entryId = it.entryId,
                        dateKey = it.dateKey,
                        historyKeyVersion = it.historyKeyVersion,
                        isCachedLocation = it.isCachedLocation,
                        history = CryptoResource.Ready(it.history)
                    )
                }
            )

            // Extract all location points, filtering by locationId and replacing with new one
            val allPoints = extractLocationPoints(toImport, locationId)
            logg.v(TAG, "Extracted ${allPoints.size} location points (filtered/replaced locationId: $locationId)")

            if (allPoints.isEmpty()) {
                logg.v(TAG, "No location points to import")
                return Result.success(Unit)
            }

            // Create new personal history key
            logg.v(TAG, "Creating new personal history key")
            val historyKeyData = createNewHistoryKey()

            // Group points by date and encrypt
            logg.v(TAG, "Grouping points by date and encrypting")
            val encryptedEntries = groupAndEncryptPoints(allPoints, historyKeyData.key, historyKeyData.historyKeyVersion)

            logg.v(TAG, "Created ${encryptedEntries.size} encrypted entries")

            // Save history key to database
            logg.v(TAG, "Saving history key to database")

            database.logoutDao.clearAllPersonalLocalHistoryKeys()
            database.logoutDao.clearAllUnconfirmedHistory()

            database.personalLocalHistoryKeyDao.saveLocalHistoryKey(
                key = PersonalLocalHistoryKeyEntity(
                    createdAt = nowUTC().toString(),
                    userKeyVersion = historyKeyData.userKeyVersion,
                    historyKeyVersion = historyKeyData.historyKeyVersion,
                    encryptedHistoryKey = historyKeyData.encryptedKey,
                    isSynced = false
                )
            )

            // Save unconfirmed entries to database
            logg.v(TAG, "Saving ${encryptedEntries.size} unconfirmed entries to database")
            database.unconfirmedLocationHistoryDao.createUnconfirmedEntries(
                entries = encryptedEntries.map {
                    UnconfirmedLocationHistoryEntity(
                        pendingUploadId = it.pendingUploadId,
                        groupId = UserGroup.groupPersonalDevicesId,
                        dateIdentifier = it.dateIdentifier,
                        locationHistoryData = it.historyData,
                        historyKeyVersion = it.historyKeyVersion
                    )
                }
            )

            logg.v(TAG, "Successfully imported location history. Entries will be synced later.")

            Result.success(Unit)
        } catch (e: Exception) {
            logg.v(TAG, "Error importing location history: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun extractLocationPoints(
        historyModel: LocationHistoryModelForUser,
        newLocationId: String
    ): List<LocationHistoryPointModel> {
        val allPoints = mutableListOf<LocationHistoryPointModel>()
        var entriesProcessed = 0
        var entriesSkipped = 0

        historyModel.history.forEach { deviceHistory ->
            // Replace locationId: process all entries regardless of original locationId
            // All entries will be associated with the new locationId when synced
            val originalLocationId = deviceHistory.locationId
            when (val cryptoResource = deviceHistory.history) {
                is CryptoResource.Ready -> {
                    val points = cryptoResource.data?.history ?: emptyList()
                    allPoints.addAll(points)
                    entriesProcessed++
                    if (points.isNotEmpty()) {
                        logg.v(TAG, "Processing entry ${deviceHistory.entryId}: ${points.size} points (original locationId: $originalLocationId -> new locationId: $newLocationId)")
                    }
                }
                is CryptoResource.UnverifiedKeyOrData -> {
                    val points = cryptoResource.data?.history ?: emptyList()
                    allPoints.addAll(points)
                    entriesProcessed++
                    if (points.isNotEmpty()) {
                        logg.v(TAG, "Processing entry ${deviceHistory.entryId}: ${points.size} points (original locationId: $originalLocationId -> new locationId: $newLocationId)")
                    }
                }
                else -> {
                    entriesSkipped++
                    logg.v(TAG, "Skipping entry ${deviceHistory.entryId} - missing or invalid key (original locationId: $originalLocationId)")
                }
            }
        }

        logg.v(TAG, "LocationId replacement complete: processed $entriesProcessed entries, skipped $entriesSkipped entries. All ${allPoints.size} points will be associated with locationId: $newLocationId")
        return allPoints
    }

    private suspend fun createNewHistoryKey(): HistoryKeyData {
        val newKey = cryptoCore.getNewSymmetricKeyForLocationData()
        val userKey = cryptoUserKeysRepo.personalKey()
        val historyKeyVersion = cryptoCore.randomId(length = 32, alphabet = AlphabetType.EXTENDED)

        val encryptedKey = cryptoCore.encryptDataWithKey(
            data = newKey,
            key = userKey.key
        )

        return HistoryKeyData(
            key = newKey,
            encryptedKey = encryptedKey,
            userKeyVersion = userKey.version,
            historyKeyVersion = historyKeyVersion
        )
    }

    private suspend fun groupAndEncryptPoints(
        points: List<LocationHistoryPointModel>,
        historyKey: String,
        historyKeyVersion: String
    ): List<EncryptedHistoryEntry> {
        // Group points by date (dayKey)
        val groupedByDate = points.groupBy { point ->
            point.dayKey ?: ""
        }.filterKeys { it.isNotBlank() }

        val encryptedEntries = mutableListOf<EncryptedHistoryEntry>()

        groupedByDate.forEach { (dateKey, datePoints) ->
            // Chunk large groups (max 100 points per entry)
            val chunks = if (datePoints.size > 120) {
                datePoints.chunked(100)
            } else {
                listOf(datePoints)
            }

            chunks.forEach { chunk ->
                val model = LocationHistoryModel(history = chunk)
                val jsonData = BaseRemoteModule.locationHistoryFormatter.encodeToString(model)
                val encryptedData = cryptoCore.encryptDataWithKey(
                    data = jsonData,
                    key = historyKey
                )
                val pendingUploadId = cryptoCore.randomId(length = 32, alphabet = AlphabetType.EXTENDED)

                encryptedEntries.add(
                    EncryptedHistoryEntry(
                        pendingUploadId = pendingUploadId,
                        dateIdentifier = dateKey,
                        historyData = encryptedData,
                        historyKeyVersion = historyKeyVersion
                    )
                )
            }
        }

        return encryptedEntries
    }

    private data class HistoryKeyData(
        val key: String,
        val encryptedKey: String,
        val userKeyVersion: Long,
        val historyKeyVersion: String
    )

    private data class EncryptedHistoryEntry(
        val pendingUploadId: String,
        val dateIdentifier: String,
        val historyData: String,
        val historyKeyVersion: String
    )
}

