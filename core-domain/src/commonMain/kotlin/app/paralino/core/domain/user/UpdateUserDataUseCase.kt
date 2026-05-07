package app.paralino.core.domain.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.domain.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ParalinoSocketConnectionType
import app.paralino.core.models.base.PushProvider
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.raw.UpdateMemberData
import app.paralino.core.models.raw.UpdateMemberFunctionPayload
import app.paralino.core.models.user.PushProviderEndpointConfig
import app.paralino.core.models.user.UserMeData
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.AppConfigRepository
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
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.days
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
    private val appConfigRepo: AppConfigRepository,
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

            val pushProvider = notificationSocketServiceManager.adjustedPushProvider()
            val wasDeviceSynced = appPrefs.wasDeviceEverSynced
            val unifiedPushPrefsUnsynced = appPrefs.unifiedPushEndpointConfigUnsynced
            val unifiedPushPrefsSynced = appPrefs.unifiedPushEndpointConfigSynced
            val currentDevicePushConfig = PushProviderEndpointConfig.get(personalSpaceRepo.personalSpaceState.value.data?.currentDevice?.pushProviderConfig)

            val pushProviderConfig = when (pushProvider) {
                PushProvider.PARALINO -> {
                    currentDevicePushConfig
                        ?.takeIf { it.distributor == PushProvider.paralinoDistributorId }
                        ?.copy(isSocketConnection = appPrefs.paralinoSocketConnectionType == ParalinoSocketConnectionType.REALTIME)
                        ?: PushProviderEndpointConfig(
                            distributor = PushProvider.paralinoDistributorId,
                            url = "",
                            id = null,
                            createdAt = null,
                            pubkey = null,
                            auth = null,
                            isSocketConnection = appPrefs.paralinoSocketConnectionType == ParalinoSocketConnectionType.REALTIME
                        )
                }

                else -> {
                    if (unifiedPushPrefsUnsynced != null) {
                        if (unifiedPushPrefsUnsynced.isBlank()) {
                            null
                        } else {
                            PushProviderEndpointConfig.get(unifiedPushPrefsUnsynced)
                        }
                    } else {
                        null
                    }
                }
            }


            val updatePushProviderConfig = when (pushProvider) {
                PushProvider.DEVICE_NATIVE -> false
                PushProvider.NOT_SET -> {
                    currentDevicePushConfig != null
                }

                PushProvider.PARALINO -> {
                    val createdAtOld = currentDevicePushConfig?.createdAt?.isoWithOffsetToInstant()?.isOlderThan(30.days) ?: true
                    val connectionTypeChanged = currentDevicePushConfig?.isSocketConnection != pushProviderConfig?.isSocketConnection
                    createdAtOld || connectionTypeChanged
                }

                PushProvider.UNIFIED_PUSH -> {
                    if (unifiedPushPrefsUnsynced != null) {
                        if (unifiedPushPrefsUnsynced.isBlank()) {
                            true
                        } else {
                            unifiedPushPrefsUnsynced != unifiedPushPrefsSynced
                        }
                    } else {
                        false
                    }
                }
            }

            val pendingState = UserDeviceDataUpdateState(
                pushToken = when (pushProvider) {
                    PushProvider.DEVICE_NATIVE -> {
                        appPrefs.pushTokenUnsynced.takeIf { it.isNotBlank() }
                            ?: appPrefs.pushTokenSynced.takeIf { it.isNotBlank() }
                    }
                    else -> null
                },
                locationPushToken = when (pushProvider) {
                    PushProvider.DEVICE_NATIVE -> {
                        appPrefs.locationPushTokenUnsynced.takeIf { it.isNotBlank() }
                            ?: appPrefs.locationPushTokenSynced.takeIf { it.isNotBlank() }
                    }
                    else -> null
                },
                pushProvider = notificationSocketServiceManager.adjustedPushProvider(),
                pushProviderConfig = pushProviderConfig,
                updatePushProviderConfig = updatePushProviderConfig
            )

            val currentState = UserDeviceDataUpdateState(
                pushToken = appPrefs.pushTokenSynced.takeIf { it.isNotBlank() },
                locationPushToken = appPrefs.locationPushTokenSynced.takeIf { it.isNotBlank() },
                pushProvider = personalSpaceRepo.personalSpaceState.value.data?.currentDevice?.pushProvider ?: PushProvider.default,
                pushProviderConfig = currentDevicePushConfig,
                updatePushProviderConfig = false
            )


            if (!pendingState.shouldUpdate(currentState) && wasDeviceSynced) {
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

                val updatedDevice = userDevicesRemoteSource.updateDeviceData(
                    pushToken = pendingState.pushToken,
                    locationPushToken = pendingState.locationPushToken,
                    pushProvider = pendingState.pushProvider.key,
                    pushProviderConfig = pendingState.pushProviderConfig,
                    updatePushProviderConfig = pendingState.updatePushProviderConfig
                )

                if (updatedDevice != null) {
                    userDeviceLocal.updateUserDeviceData(device = updatedDevice)
                }

                appPrefs.pushTokenUnsynced = ""
                appPrefs.pushTokenSynced = pendingState.pushToken ?: ""

                appPrefs.locationPushTokenUnsynced = ""
                appPrefs.locationPushTokenSynced = pendingState.locationPushToken ?: ""

                appPrefs.unifiedPushEndpointConfigUnsynced = null
                appPrefs.unifiedPushEndpointConfigSynced = if (pendingState.updatePushProviderConfig) unifiedPushPrefsUnsynced else unifiedPushPrefsSynced

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

    private data class UserDeviceDataUpdateState(
        val pushToken: String?,
        val locationPushToken: String?,
        val pushProvider: PushProvider,
        val updatePushProviderConfig: Boolean,
        val pushProviderConfig: PushProviderEndpointConfig?,
    ) {
        fun shouldUpdate(other: UserDeviceDataUpdateState): Boolean {
            val didConfigChange = if (this.pushProviderConfig != null) {
                this.pushProviderConfig != other.pushProviderConfig
            } else {
                other.pushProviderConfig != null && this.updatePushProviderConfig
            }
            return this.pushToken != other.pushToken
                    || this.locationPushToken != other.locationPushToken
                    || this.pushProvider != other.pushProvider
                    || this.updatePushProviderConfig != other.updatePushProviderConfig
                    || didConfigChange
        }
    }
}