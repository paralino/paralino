package app.paralino.core.domain.location

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.PersonalSpaceDeviceKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.groups.MemberLocationEntityDataUpdate
import app.paralino.core.db.main.user.UserDeviceEntityLocationDataUpdate
import app.paralino.core.domain.location.history.LocationHistoryUploadEngine
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.location.MemberGroupIdForUpload
import app.paralino.core.models.mappers.PendingLocationHistoryMapper
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.UserDeviceLocationRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes

class SaveLocationUseCase(
    private val database: MainDatabaseHelper,
    private val documentApi: IDocumentsApi,
    private val appBuildConfig: AppBuildConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val historyMapper: PendingLocationHistoryMapper,
    private val locationHistoryUpload: LocationHistoryUploadEngine,
    private val appPrefs: AppPrefs
) {
    @Throws(CryptoException::class, MissingCryptoKeyException::class, CancellationException::class)
    suspend fun uploadAndSaveCurrentLocation(data: MemberGroupIdForUpload) {
        if (data.groupId.isPersonalSpace) {
            uploadAndSavePersonalLocation(data = data)
        } else {
            uploadAndSaveGroupLocation(data = data)
        }
    }

    @Throws(CryptoException::class, MissingCryptoKeyException::class, CancellationException::class)
    private suspend fun uploadAndSavePersonalLocation(data: MemberGroupIdForUpload): UserDeviceLocationRaw? {
        val deviceKey = userKeyRepo.getDeviceSenderKeyForLocationId(
            locationId = data.locationId
        )
        if (deviceKey !is PersonalSpaceDeviceKey.Valid)
            throw MissingMemberKeyException(groupId = data.groupId.id, memberId = appPrefs.userId, keyError = deviceKey::class.simpleName)

        val locationPayload = getLocationPayload(
            data = data,
            key = deviceKey.key
        )

        val historyPayload = getLocationHistoryPayload(
            data = data,
            key = deviceKey.key
        )

        val payload = buildMap {
            putAll(locationPayload)
            put("locationHistory", historyPayload)
        }.filter { it.value != null }

        if (payload.isEmpty()) {
            return null
        }

        val newData = documentApi.updateDocument(
            databaseType = BackendDatabaseType.DATA,
            collectionId = appBuildConfig.resourceIds.collectionPersonalLocationsID,
            docId = data.locationId,
            data = payload
        ).let { UserDeviceLocationRaw.from(it) }

        savePersonalLocationLocally(newData)

        if ((historyPayload?.length ?: 0) >= 50_000) {
            locationHistoryUpload.syncPendingEntriesInBackground(cacheTime = 2.minutes)
        }

        return newData
    }

    @Throws(CryptoException::class, MissingCryptoKeyException::class, CancellationException::class)
    private suspend fun uploadAndSaveGroupLocation(data: MemberGroupIdForUpload): MemberLocationRaw? {
        val memberKey = cryptoKEKRepo.getMemberSenderKey(
            groupId = data.groupId.id,
            memberUserId = appPrefs.userId,
            locationId = data.locationId
        )
        if (memberKey !is GroupMemberSenderKey.Valid)
            throw MissingMemberKeyException(groupId = data.groupId.id, memberId = appPrefs.userId)

        val locationPayload = getLocationPayload(
            data = data,
            key = memberKey.key
        )

        val historyPayload = getLocationHistoryPayload(
            data = data,
            key = memberKey.key
        )

        val payload = buildMap {
            putAll(locationPayload)
            put("locationHistory", historyPayload)
        }.filter { it.value != null }

        if (payload.isEmpty()) {
            return null
        }

        val payloadWithKey = payload.plus(
            "deviceKeyVersion" to memberKey.deviceKeyVersion
        )

        val newData = documentApi.updateDocument(
            databaseType = BackendDatabaseType.DATA,
            collectionId = appBuildConfig.resourceIds.collectionMemberLocationsID,
            docId = data.locationId,
            data = payloadWithKey
        ).let { MemberLocationRaw.from(it) }

        saveGroupLocationLocally(newData)

        if ((historyPayload?.length ?: 0) >= 50_000) {
            locationHistoryUpload.syncPendingEntriesInBackground(cacheTime = 2.minutes)
        }

        return newData
    }

    private suspend fun getLocationPayload(
        data: MemberGroupIdForUpload,
        key: String
    ): Map<String, String?> {
        val locationData = data.locationData?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = key
            )
        }

        val status = data.deviceStatus?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = key
            )
        }

        return mapOf(
            "locationData" to locationData,
            "status" to status,
        )
    }

    private suspend fun getLocationHistoryPayload(
        data: MemberGroupIdForUpload,
        key: String
    ): String? {

        val historyEntries = if (data.groupId.isPersonalSpace) {
            database.pendingLocationHistoryDao.getAllEntriesForGroupOnce(groupOrSpaceId = data.groupId.id)
        } else {
            val historyKey = database.groupLocalHistoryKeyDao.getActiveHistoryKey(groupId = data.groupId.id)
            database.pendingLocationHistoryDao.getAllEntriesForGroupOnce(groupOrSpaceId = data.groupId.id)
                // history key can be null for personal space
                .filter { it.localGroupHistoryKeyId == historyKey?.id }
        }
            .sortedByDescending { it.id }
            .take(100)
            .let { historyMapper.pendingLocationToCore(it) }

        val locationHistory = if (historyEntries.isNotEmpty()) {
            val historyModel = LocationHistoryModel(
                history = historyEntries.map { it.history }
            )
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.locationHistoryFormatter.encodeToString(historyModel),
                key = key
            )
        } else {
            ""
        }

        return locationHistory
    }

    suspend fun saveGroupLocationLocally(data: MemberLocationRaw) {
        logg.v("Save new group location to database (${data.id})")
        database.groupRelatedDao.updateMemberLocation(
            MemberLocationEntityDataUpdate(
                id = data.id,
                deviceId = data.deviceId,
                groupKeyVersion = data.groupKeyVersion,
                deviceKeyVersion = data.deviceKeyVersion,
                deviceData = data.deviceData,
                locationData = data.locationData,
                status = data.status,
                locationHistory = data.locationHistory,
            )
        )
    }

    suspend fun savePersonalLocationLocally(data: UserDeviceLocationRaw) {
        logg.v("Save new personal location to database (${data.id})")
        database.userDeviceDao.updateUserDeviceLocationData(
            UserDeviceEntityLocationDataUpdate(
                id = data.id,
                locationId = data.id,
                locationData = data.locationData,
                status = data.status,
                locationHistory = data.locationHistory,
            )
        )
    }
}