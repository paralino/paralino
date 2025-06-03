package app.paralino.core.domain.location

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.groups.MemberLocationEntityDataUpdate
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.mappers.PersonalLocationHistoryMapper
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import kotlin.coroutines.cancellation.CancellationException

class SaveLocationUseCase(
    private val database: MainDatabaseHelper,
    private val documentApi: IDocumentsApi,
    private val appBuildConfig: AppBuildConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val historyMapper: PersonalLocationHistoryMapper,
    private val appPrefs: AppPrefs
) {
    @Throws(CryptoException::class, MissingCryptoKeyException::class, CancellationException::class)
    suspend fun uploadAndSaveCurrentLocation(data: MemberGroupId): MemberLocationRaw? {
        val memberKey = cryptoKEKRepo.getMemberSenderKey(
            groupId = data.groupId,
            memberUserId = appPrefs.userId,
            locationId = data.locationId
        )
        if (memberKey !is GroupMemberSenderKey.Valid)
            throw MissingMemberKeyException(data.groupId, appPrefs.userId)

        val locationData = data.locationData?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = memberKey.key
            )
        }

        val historyKey = database.pendingLocationHistoryDao.getLatestHistoryKey(groupId = data.groupId)
        val historyEntries = database.pendingLocationHistoryDao.getAllEntriesForGroupOnce(groupId = data.groupId)
            .filter { it.localHistoryKeyId == historyKey?.id }
            .sortedByDescending { it.id }
            .take(100)
            .let { historyMapper.pendingLocationToCore(it) }

        val historyModel = LocationHistoryModel(
            history = historyEntries.map { it.history }
        )

        val locationHistory = cryptoCore.encryptDataWithKey(
            data = BaseRemoteModule.locationHistoryFormatter.encodeToString(historyModel),
            key = memberKey.key
        )

        val status = data.deviceStatus?.let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = memberKey.key
            )
        }

        val payload = mapOf(
            "locationData" to locationData,
            "locationHistory" to locationHistory,
            "status" to status,
            "deviceKeyVersion" to memberKey.deviceKeyVersion,
        ).filter { it.value != null }

        if (payload.isEmpty()) {
            return null
        }

        val newData = documentApi.updateDocument(
            databaseType = BackendDatabaseType.DATA,
            collectionId = appBuildConfig.resourceIds.collectionMemberLocationsID,
            docId = data.locationId,
            data = payload
        ).let { MemberLocationRaw.from(it) }

        saveLocationLocally(newData)

        return newData
    }

    suspend fun saveLocationLocally(data: MemberLocationRaw) {
        logg.v("Save new location to database (${data.id})")
        database.groupRelatedDao.updateMemberLocation(
            MemberLocationEntityDataUpdate(
                id = data.id,
                deviceId = data.deviceId,
                deviceData = data.deviceData,
                groupKeyVersion = data.groupKeyVersion,
                deviceKeyVersion = data.deviceKeyVersion,
                locationData = data.locationData,
                locationHistory = data.locationHistory,
                status = data.status
            )
        )
    }
}