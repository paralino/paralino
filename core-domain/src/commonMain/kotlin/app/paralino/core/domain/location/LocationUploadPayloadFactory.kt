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
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.MemberGroupIdForUpload
import app.paralino.core.models.mappers.PendingLocationHistoryMapper
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * A `null` field means do not send / do not touch locally.
 */
sealed class PreparedLocationUpload {
    abstract val item: MemberGroupIdForUpload
    abstract val locationData: String?
    abstract val status: String?
    abstract val locationHistory: String?
    abstract val pendingHistoryCount: Int
    abstract val oldestEntryTimestamp: Instant?

    val locationId: String get() = item.locationId

    val shouldTriggerHistorySync: Boolean
        get() = (locationHistory?.length ?: 0) >= 50_000
            || pendingHistoryCount > 150
            || oldestEntryTimestamp?.isOlderThan(1.days) == true

    data class Personal(
        override val item: MemberGroupIdForUpload,
        override val locationData: String?,
        override val status: String?,
        override val locationHistory: String?,
        override val pendingHistoryCount: Int,
        override val oldestEntryTimestamp: Instant?
    ) : PreparedLocationUpload()

    data class Group(
        override val item: MemberGroupIdForUpload,
        override val locationData: String?,
        override val status: String?,
        override val locationHistory: String?,
        override val pendingHistoryCount: Int,
        override val oldestEntryTimestamp: Instant?,
        val deviceKeyVersion: Long
    ) : PreparedLocationUpload()
}

private data class LocationHistoryPreparePayload(
    val locationHistory: String?,
    val pendingHistoryCount: Int,
    val oldestEntryTimestamp: Instant?
)

class LocationUploadPayloadFactory(
    private val database: MainDatabaseHelper,
    private val appConfig: AppConfigRepository,
    private val cryptoCore: CryptoCore,
    private val getMemberSenderKey: GetMemberSenderKeyUseCase,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val historyMapper: PendingLocationHistoryMapper,
    private val appPrefs: AppPrefs,
    private val locationPerformanceManager: LocationPerformanceManager
) {
    private val lastEmbeddedHistoryUpload = MutableStateFlow<Map<String, Instant>>(emptyMap())

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
            val memberKey = getMemberSenderKey.getCurrentUserDeviceSenderKeyForReadWrite(
                groupId = data.groupId.id,
                locationId = data.locationId
            )
            if (memberKey !is GroupMemberSenderKey.ValidForReadWrite) {
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

        if (locationData == null && status == null && historyPayload.locationHistory == null) {
            return null
        }

        return if (data.groupId.isPersonalSpace) {
            PreparedLocationUpload.Personal(
                item = data,
                locationData = locationData,
                status = status,
                locationHistory = historyPayload.locationHistory,
                pendingHistoryCount = historyPayload.pendingHistoryCount,
                oldestEntryTimestamp = historyPayload.oldestEntryTimestamp
            )
        } else {
            if (deviceKeyVersion != null) {
                PreparedLocationUpload.Group(
                    item = data,
                    locationData = locationData,
                    status = status,
                    locationHistory = historyPayload.locationHistory,
                    pendingHistoryCount = historyPayload.pendingHistoryCount,
                    oldestEntryTimestamp = historyPayload.oldestEntryTimestamp,
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
    ): LocationHistoryPreparePayload {
        when (appConfig.appHost) {
            AppHost.MAIN_APP -> {
                if (!shouldAttachEmbeddedHistory(data)) {
                    return skippedEmbeddedHistory
                }

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

                val oldestEntryTimestamp = historyEntries
                    .mapNotNull { it.history.status.startTime.fromUTCtoInstant() }
                    .minOrNull()

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

                lastEmbeddedHistoryUpload.update { it + (data.groupId.id to nowUTC()) }

                return LocationHistoryPreparePayload(
                    locationHistory = locationHistory,
                    pendingHistoryCount = allPendingEntries.size,
                    oldestEntryTimestamp = oldestEntryTimestamp
                )
            }

            AppHost.IOS_NOTIFICATION_EXTENSION,
            AppHost.IOS_LOCATION_EXTENSION -> {
                return skippedEmbeddedHistory
            }
        }
    }

    private fun shouldAttachEmbeddedHistory(data: MemberGroupIdForUpload): Boolean {
        val isHistoryOnlyUpload = data.uploadPendingHistory
                && data.locationData == null
                && data.deviceStatus == null
        if (isHistoryOnlyUpload) {
            return true
        }

        val lastUpload = lastEmbeddedHistoryUpload.value[data.groupId.id] ?: return true
        return lastUpload.isOlderThan(embeddedHistoryMinInterval())
    }

    private fun embeddedHistoryMinInterval(): Duration {
        return when (locationPerformanceManager.locationPerformanceType.value.current) {
            LocationPerformanceType.HIGH_BATTERY_SAVER,
            LocationPerformanceType.BATTERY_SAVER,
            LocationPerformanceType.BALANCED -> 1.minutes
            LocationPerformanceType.MEDIUM_HIGH -> 30.seconds
            LocationPerformanceType.HIGH -> {
                when (locationPerformanceManager.locationPerformanceType.value.balancedPerformanceSettings.config) {
                    LocationPerformanceSettingsType.Balanced.Level1 -> 30.seconds
                    LocationPerformanceSettingsType.Balanced.Level2 -> 25.seconds
                    LocationPerformanceSettingsType.Balanced.Level3 -> 20.seconds
                    LocationPerformanceSettingsType.Balanced.Level4 -> 15.seconds
                    LocationPerformanceSettingsType.Balanced.Level5 -> 10.seconds
                }
            }
            LocationPerformanceType.HIGHER_FOCUS,
            LocationPerformanceType.VERY_HIGH,
            LocationPerformanceType.MANUAL_BOOST -> {
                when (locationPerformanceManager.locationPerformanceType.value.balancedPerformanceSettings.config) {
                    LocationPerformanceSettingsType.Balanced.Level1 -> 25.seconds
                    LocationPerformanceSettingsType.Balanced.Level2 -> 20.seconds
                    LocationPerformanceSettingsType.Balanced.Level3 -> 15.seconds
                    LocationPerformanceSettingsType.Balanced.Level4 -> 10.seconds
                    LocationPerformanceSettingsType.Balanced.Level5 -> 5.seconds
                }
            }
        }
    }

    companion object {
        private val skippedEmbeddedHistory = LocationHistoryPreparePayload(
            locationHistory = null,
            pendingHistoryCount = 0,
            oldestEntryTimestamp = null
        )
    }
}
