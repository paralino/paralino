package app.paralino.android.domain

import app.paralino.android.AppClass
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationHistoryModelForUser
import app.paralino.core.repo.user.ExportableLocationHistoryModelForDevice
import app.paralino.core.repo.user.ExportableLocationHistoryModelForUser
import app.paralino.core.repo.user.IExportPersonalLocationHistoryUseCase
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.extensions.historyDateKeyForApi
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.io.File
import kotlin.time.Duration


class ExportPersonalLocationHistoryUseCase(
    private val appClass: AppClass,
    private val refreshLocationHistoryUseCase: RefreshLocationHistoryUseCase,
    private val getLocationHistoryUseCase: GetLocationHistoryUseCase,
    private val appPrefs: AppPrefs
): IExportPersonalLocationHistoryUseCase {

    companion object {
        private const val TAG = "export_location_history"
        private const val BATCH_SIZE_DAYS = 15
    }

    override suspend fun exportPersonalLocationHistoryUntil(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<Boolean> {
        return try {
            logg.v(TAG, "Starting export of personal location history until $endDate")

            // Fetch from 1 year before endDate to endDate

            logg.v(TAG, "Fetching history from $startDate to $endDate")

            // Fetch history in batches of 15 days
            fetchHistoryInBatches(startDate = startDate, endDate = endDate)

            // Get all history from database
            logg.v(TAG, "Retrieving all history from database")
            val history = getLocationHistoryUseCase.getLocationHistory(
                groupOrSpaceId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
                memberUserId = appPrefs.userId,
                startDate = startDate,
                endDate = endDate
            ).first()

            logg.v(TAG, "Retrieved ${history.history.size} history entries")

            // Save to JSON file
            val file = saveHistoryToJsonFile(history, endDate)

            logg.v(TAG, "Successfully exported history to ${file.absolutePath}")

            Result.success(true)
        } catch (e: Exception) {
            logg.v(TAG, "Error exporting location history: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchHistoryInBatches(startDate: LocalDate, endDate: LocalDate) {
        var currentStartDate = startDate
        var batchNumber = 1

        while (currentStartDate <= endDate) {
            val batchEndDate = minOf(
                currentStartDate.plus(DatePeriod(days = BATCH_SIZE_DAYS - 1)),
                endDate
            )

            logg.v(TAG, "Fetching batch $batchNumber: $currentStartDate to $batchEndDate")

            refreshLocationHistoryUseCase.refreshLocationHistory(
                groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
                memberUserId = appPrefs.userId,
                locationId = null,
                startDate = currentStartDate,
                endDate = batchEndDate,
                requestCacheDuration = Duration.ZERO // Force fresh fetch
            )

            logg.v(TAG, "Completed batch $batchNumber")

            currentStartDate = batchEndDate.plus(DatePeriod(days = 1))
            batchNumber++
        }

        logg.v(TAG, "Completed all batches. Total batches: ${batchNumber - 1}")
    }

    private fun saveHistoryToJsonFile(
        history: LocationHistoryModelForUser,
        endDate: LocalDate
    ): File {
        logg.v(TAG, "Serializing history to JSON")

        val toExport = ExportableLocationHistoryModelForUser(
            groupId = history.groupId,
            memberUserId = history.memberUserId,
            history = history.history.mapNotNull {
                it.history.data?.let { it1 ->
                    ExportableLocationHistoryModelForDevice(
                        locationId = it.locationId,
                        entryId = it.entryId,
                        dateKey = it.dateKey,
                        historyKeyVersion = it.historyKeyVersion,
                        isCachedLocation = it.isCachedLocation,
                        history = it1
                    )
                }
            }
        )

        val jsonString = BaseRemoteModule.prettyPrintFormatter.encodeToString(toExport)
            ?: throw IllegalStateException("Failed to serialize history to JSON")

        logg.v(TAG, "JSON size: ${jsonString.length} characters")

        val fileName = "personal_location_history_until_${endDate.historyDateKeyForApi()}.json"
        val file = File(appClass.filesDir, fileName)

        logg.v(TAG, "Writing JSON to file: ${file.absolutePath}")

        file.writeText(jsonString)

        logg.v(TAG, "File written successfully. Size: ${file.length()} bytes")

        return file
    }
}

