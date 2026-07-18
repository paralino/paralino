package app.paralino.core.domain.location

import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.PersonalSpaceDeviceKey
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.domain.crypto.group.GetMemberSenderKeyUseCase
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.models.AppHost
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.MemberGroupIdForUpload
import app.paralino.core.models.mappers.PendingLocationHistoryMapper
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import kotlin.coroutines.cancellation.CancellationException

/**
 * A `null` field means do not send / do not touch locally.
 */
sealed class PreparedLocationUpload {
    abstract val item: MemberGroupIdForUpload
    abstract val locationData: String?
    abstract val status: String?
    abstract val locationHistory: String?
    abstract val pendingHistoryCount: Int

    val locationId: String get() = item.locationId

    val shouldTriggerHistorySync: Boolean
        get() = (locationHistory?.length ?: 0) >= 50_000 || pendingHistoryCount > 150

    data class Personal(
        override val item: MemberGroupIdForUpload,
        override val locationData: String?,
        override val status: String?,
        override val locationHistory: String?,
        override val pendingHistoryCount: Int
    ) : PreparedLocationUpload()

    data class Group(
        override val item: MemberGroupIdForUpload,
        override val locationData: String?,
        override val status: String?,
        override val locationHistory: String?,
        override val pendingHistoryCount: Int,
        val deviceKeyVersion: Long
    ) : PreparedLocationUpload()
}

class LocationUploadPayloadFactory(
    private val database: MainDatabaseHelper,
    private val appConfig: AppConfigRepository,
    private val cryptoCore: CryptoCore,
    private val getMemberSenderKey: GetMemberSenderKeyUseCase,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val historyMapper: PendingLocationHistoryMapper,
    private val appPrefs: AppPrefs
) {

    /**
     * Resolves the sender key and encrypts location data, status and pending history for one location.
     * null -> there is nothing to upload.
     */
    @Throws(CryptoException::class, MissingCryptoKeyException::class, CancellationException::class)
    suspend fun prepare(data: MemberGroupIdForUpload): PreparedLocationUpload? {
        val key: String
        val deviceKeyVersion: Long?

        if (data.groupId.isPersonalSpace) {
            val deviceKey = userKeyRepo.getDeviceSenderKeyForLocationId(
                locationId = data.locationId
            )

            if (deviceKey !is PersonalSpaceDeviceKey.Valid) {
                throw MissingMemberKeyException(
                    groupId = data.groupId.id,
                    memberId = appPrefs.userId,
                    keyError = deviceKey::class.simpleName
                )
            }

            key = deviceKey.key
            deviceKeyVersion = null
        } else {
            val memberKey = getMemberSenderKey.getCurrentUserDeviceSenderKey(
                groupId = data.groupId.id,
                locationId = data.locationId
            )
            if (memberKey !is GroupMemberSenderKey.Valid) {
                throw MissingMemberKeyException(
                    groupId = data.groupId.id,
                    memberId = appPrefs.userId
                )
            }

            key = memberKey.key
            deviceKeyVersion = memberKey.deviceKeyVersion
        }

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

        val historyPayload = getLocationHistoryPayload(
            data = data,
            key = key
        )

        if (locationData == null && status == null && historyPayload.first == null) {
            return null
        }

        return if (data.groupId.isPersonalSpace) {
            PreparedLocationUpload.Personal(
                item = data,
                locationData = locationData,
                status = status,
                locationHistory = historyPayload.first,
                pendingHistoryCount = historyPayload.second
            )
        } else {
            if (deviceKeyVersion != null) {
                PreparedLocationUpload.Group(
                    item = data,
                    locationData = locationData,
                    status = status,
                    locationHistory = historyPayload.first,
                    pendingHistoryCount = historyPayload.second,
                    deviceKeyVersion = deviceKeyVersion
                )
            } else {
                null
            }
        }
    }

    private suspend fun getLocationHistoryPayload(
        data: MemberGroupIdForUpload,
        key: String
    ): Pair<String?, Int> {
        when (appConfig.appHost) {
            AppHost.MAIN_APP -> {
                val allPendingEntries = if (data.groupId.isPersonalSpace) {
                    database.pendingLocationHistoryDao.getAllEntriesForGroupOnce(groupOrSpaceId = data.groupId.id)
                } else {
                    val historyKey = database.groupLocalHistoryKeyDao.getActiveHistoryKey(groupId = data.groupId.id)
                    database.pendingLocationHistoryDao.getAllEntriesForGroupOnce(groupOrSpaceId = data.groupId.id)
                        // history key can be null for personal space
                        .filter { it.localGroupHistoryKeyId == historyKey?.id }
                }
                    .sortedByDescending { it.id }

                val historyEntries = allPendingEntries
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

                return locationHistory to allPendingEntries.size
            }

            AppHost.IOS_NOTIFICATION_EXTENSION,
            AppHost.IOS_LOCATION_EXTENSION -> {
                return null to 0
            }
        }
    }
}
