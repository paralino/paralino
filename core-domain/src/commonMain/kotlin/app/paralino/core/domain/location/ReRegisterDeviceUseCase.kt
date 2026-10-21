package app.paralino.core.domain.location

import app.paralino.core.MR
import app.paralino.core.domain.notifications.IInfoMessagesNotificationPresenter
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.LocationUpdateDataFromDevice
import app.paralino.core.provider.localizedString
import app.paralino.core.utils.exceptions.DeviceSafetySignatureCorrupted
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

abstract class IReRegisterDeviceUseCase {
    abstract suspend fun tryRegisterDeviceAgain(
        groupId: GroupOrSpaceId,
        data: LocationUpdateDataFromDevice?,
        contract: LocationSharingContract,
    ): Boolean

    abstract fun logout()
}

class NoOpReRegisterDeviceUseCase: IReRegisterDeviceUseCase() {
    override suspend fun tryRegisterDeviceAgain(
        groupId: GroupOrSpaceId,
        data: LocationUpdateDataFromDevice?,
        contract: LocationSharingContract
    ): Boolean {
        return false
    }

    override fun logout() {}
}

class ReRegisterDeviceUseCase(
    private val registerDeviceForSharing: RegisterDeviceForSharingUseCase,
    private val infoNotificationPresenter: IInfoMessagesNotificationPresenter,
): IReRegisterDeviceUseCase() {

    private val deviceRegisterTimeMillis = MutableStateFlow(mapOf<GroupOrSpaceId, Long>())
    private var failedRegistrationAttempts = MutableStateFlow(mapOf<GroupOrSpaceId, Int>())

    override suspend fun tryRegisterDeviceAgain(
        groupId: GroupOrSpaceId,
        data: LocationUpdateDataFromDevice?,
        contract: LocationSharingContract,
    ): Boolean {
        try {
            val timeBetweenRequests = nowUTC() - Instant.fromEpochMilliseconds(
                epochMilliseconds = deviceRegisterTimeMillis.value[groupId] ?: 0
            )
            if (timeBetweenRequests > 10.minutes) {
                logg.v(
                    "location_service",
                    "Missing user location document for new location, try registering device"
                )
                failedRegistrationAttempts.update {
                    it.toMutableMap().apply { set(groupId, 0) }
                }
                deviceRegisterTimeMillis.update {
                    it.toMutableMap().apply { set(groupId, nowUTC().toEpochMilliseconds()) }
                }
                registerDeviceForSharing.registerDevice(
                    groupId = groupId,
                    deviceName = null,
                    contract = contract,
                    notifyOthers = false,
                    isRetry = true
                )
                logg.v("location_service", "Device registered, retry payload")
                return true
            } else {
                failedRegistrationAttempts.update {
                    it.toMutableMap().apply { set(groupId, this[groupId]?.plus(1) ?: 1) }
                }
                if ((failedRegistrationAttempts.value[groupId] ?: 0) > 3) {
                    failedRegistrationAttempts.update {
                        it.toMutableMap().apply { set(groupId, 0) }
                    }
                    infoNotificationPresenter.notify(
                        IPushNotificationPresenter.Payload(
                            notificationId = 54343,
                            notificationGroupId = IPushNotificationPresenter.notificationGroupSystem,
                            title = MR.strings.error_something_wrong.localizedString(),
                            message = MR.strings.error_device_registration_notification_message.localizedString(),
                            timeMillis = null,
                            timeInfo = null
                        )
                    )
                }
                return false
            }
        } catch (e: Exception) {
            logg.w("location_service", "Error registering device for group sharing", e)
            infoNotificationPresenter.notify(
                IPushNotificationPresenter.Payload(
                    notificationId = 54343,
                    notificationGroupId = IPushNotificationPresenter.notificationGroupSystem,
                    title = MR.strings.error_something_wrong.localizedString(),
                    message = MR.strings.error_device_registration_notification_message.localizedString(),
                    timeMillis = null,
                    timeInfo = null
                )
            )

            if (e is DeviceSafetySignatureCorrupted) {
                failedRegistrationAttempts.update {
                    it.toMutableMap().apply { set(groupId, 0) }
                }
                deviceRegisterTimeMillis.update {
                    it.toMutableMap().apply { set(groupId, 0) }
                }
                throw e
            }

            return false
        }
    }

    override fun logout() {
        failedRegistrationAttempts.update { emptyMap() }
        deviceRegisterTimeMillis.update { emptyMap() }
    }
}