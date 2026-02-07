package app.paralino.core.domain.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.raw.UpdateMemberData
import app.paralino.core.models.raw.UpdateMemberFunctionPayload
import app.paralino.core.models.user.UserMeData
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.PersonalDevicesLocalDataSource
import app.paralino.core.repo.user.UserDevicesRemoteSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRemoteSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds

class UpdateUserDataUseCase constructor(
    private val userRepo: UserRepository,
    private val userRemoteSource: UserRemoteSource,
    private val userDevicesRemoteSource: UserDevicesRemoteSource,
    private val groupLocal: UserGroupLocalDataSource,
    private val functionsApi: IFunctionsApi,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val personalCodes: PersonalCodesRepository,
    private val appPrefs: AppPrefs,
    private val notificationSocketServiceManager: NotificationSocketServiceManager,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val userDeviceLocal: PersonalDevicesLocalDataSource
) : LimitedExecutionRepository() {

    fun updateUserData(data: UserMeData): Flow<Unit> {
        return getDataOnce(
            remote = {
                val newData = userRemoteSource.updateUserData(data)
                updateMemberDataInGroups(data)
                personalCodes.updateUserCode(data)
                newData
            },
            localUpdate = {
                userRepo.updateLocalUser(it)
            }
        )
            .startProcessing("update_user_data")
            .map { Unit }
    }

    private suspend fun updateMemberDataInGroups(data: UserMeData) {
        val userMembers = groupLocal.getMemberModelsForUser(appPrefs.userId)

        val nodes = userMembers.map { member ->

            val groupKey = cryptoKEKRepo.getGroupKey(
                groupId = member.groupId,
                keyVersion = member.groupKeyVersion
            )

            if (groupKey !is GroupMasterKey.Valid)
                throw MissingGroupKeyException(member.groupId, member.groupKeyVersion)

            val memberData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(
                    MemberModelData(data.userName, data.userImage)
                ),
                key = groupKey.key
            )

            UpdateMemberData(
                memberId = member.id,
                groupKeyVersion = member.groupKeyVersion,
                memberData = memberData,
            )
        }

        if (nodes.isEmpty())
            return

        val payload = UpdateMemberFunctionPayload(
            data = nodes
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.MEMBER_UPDATE,
            data = payload
        )

        if (!response.isSuccess) {
            throw response.remoteException
        }
    }

    suspend fun registerPushTokenIfNeeded(): Payload<Unit>? {

        try {
            startProcessing("notification_token_update")
        } catch (e: Exception) {
            logg.tag("push_notifications")
                .v("Notification token update already in progress")
            return Payload.Loading
        }

        try {
            userRepo.cachedUser?.data ?: let {
                logg.tag("push_notifications").v("User not present, ignoring...")
                return null
            }

            val hasNewLocationPushToken = appPrefs.locationPushTokenUnsynced
                .takeIf { it.isNotBlank() && it != appPrefs.locationPushTokenSynced }
                ?.isNotBlank()
                ?: false

            val hasNewPushToken = appPrefs.pushTokenUnsynced
                .takeIf { it.isNotBlank() && it != appPrefs.pushTokenSynced }
                ?.isNotBlank()
                ?: false

            val wasDeviceSynced = appPrefs.wasDeviceEverSynced
            val isSocketServiceActive = notificationSocketServiceManager.isServiceActive()

            val wasSocketServiceStatusChanged = personalSpaceRepo.personalSpaceState.value
                .data?.currentDevice?.let {
                    if (it.isUsingNotificationSockets == null) {
                        // if status is not initialized yet, only consider updating it if socket is active
                        // otherwise no need to call the api again
                        isSocketServiceActive
                    } else {
                        it.isUsingNotificationSockets != isSocketServiceActive
                    }
                } ?: false

            if (!hasNewPushToken && !hasNewLocationPushToken && !wasSocketServiceStatusChanged && wasDeviceSynced) {
                logg.tag("push_notifications")
                    .v("No new push token to sync, device was already synced")
                return null
            }

            logg.tag("push_notifications").i("Found unsynced tokens, syncing...")

            triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(
                    key = "push_token_sync",
                    timeMillis = 5.seconds.inWholeMilliseconds
                )
            ) {
                val newPushToken = appPrefs.pushTokenUnsynced.takeIf { it.isNotBlank() }
                    ?: appPrefs.pushTokenSynced.takeIf { it.isNotBlank() }

                if (newPushToken.isNullOrBlank() && !wasSocketServiceStatusChanged && wasDeviceSynced) {
                    return@triggerRemoteMaybe null
                }

                val newLocationPushToken =
                    appPrefs.locationPushTokenUnsynced.takeIf { it.isNotBlank() }
                        ?: appPrefs.locationPushTokenSynced.takeIf { it.isNotBlank() }
                val updatedDevice = userDevicesRemoteSource.updateDeviceData(
                    pushToken = newPushToken,
                    locationPushToken = newLocationPushToken,
                    isUsingNotificationSockets = isSocketServiceActive
                )

                if (updatedDevice != null) {
                    userDeviceLocal.updateUserDeviceData(device = updatedDevice)
                }

                appPrefs.pushTokenUnsynced = ""
                appPrefs.pushTokenSynced = newPushToken ?: ""

                appPrefs.locationPushTokenUnsynced = ""
                appPrefs.locationPushTokenSynced = newLocationPushToken ?: ""

                appPrefs.wasDeviceEverSynced = true

                logg.tag("push_notifications").i("Push token registered")
            }
            return Payload.Success(Unit)
        } catch (e: Exception) {
            return Payload.Error(e)
        } finally {
            stopProcessing("notification_token_update")
        }
    }
}