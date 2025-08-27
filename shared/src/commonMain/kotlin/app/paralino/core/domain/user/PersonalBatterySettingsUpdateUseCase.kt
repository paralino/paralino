package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.models.group.BatteryNotificationsSettingsData
import app.paralino.core.models.group.BatteryNotificationsSettingsUserEntry
import app.paralino.core.models.raw.PersonalBatteryNotificationsUpdatePayload
import app.paralino.core.models.raw.UserDeviceRaw
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.user.PersonalDevicesLocalDataSource
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PersonalBatterySettingsUpdateUseCase(
    private val functionsApi: IFunctionsApi,
    private val userDeviceLocal: PersonalDevicesLocalDataSource,
    private val appPrefs: AppPrefs
): LimitedExecutionRepository() {

    fun updateBatterySettings(
        active: Boolean
    ): Flow<Unit> {
        return getDataOnce(
            remote = {
                val device = userDeviceLocal.getUserDeviceByDeviceIdOnce(deviceId = appPrefs.deviceId)
                val currentSettings = device?.batteryAlertSettings

                val settings = if (currentSettings != null) {
                    currentSettings.copy(isActive = active)
                } else {
                    BatteryNotificationsSettingsData(
                        isActive = active,
                        settings = emptyList()
                    )
                }
                updateBatteryNotifications(batterySettings = settings)
            },
            localUpdate = {
                userDeviceLocal.updateUserDeviceData(it)
            }
        )
            .startProcessing("battery_settings")
            .map { Unit }
    }

    fun updateBatterySettings(
        data: BatteryNotificationsSettingsUserEntry
    ): Flow<Unit> {
        return getDataOnce(
            remote = {
                val device = userDeviceLocal.getUserDeviceByDeviceIdOnce(deviceId = appPrefs.deviceId)
                val currentSettings = device?.batteryAlertSettings

                val settings = if (currentSettings != null) {
                    val currentAlert = currentSettings.settings.firstOrNull {
                        it.deviceId == data.deviceId
                    }

                    val newAlerts = if (currentAlert != null) {
                        currentSettings.settings.toMutableList().apply {
                            removeAll { it.deviceId == data.deviceId }
                            add(data)
                        }
                    } else {
                        currentSettings.settings.toMutableList().apply { add(data) }
                    }

                    currentSettings.copy(settings = newAlerts)
                } else {
                    BatteryNotificationsSettingsData(
                        isActive = true,
                        settings = listOf(data)
                    )
                }
                updateBatteryNotifications(
                    batterySettings = settings
                )
            },
            localUpdate = {
                userDeviceLocal.updateUserDeviceData(it)
            }
        )
            .startProcessing("battery_settings")
            .map { Unit }
    }

    private suspend fun updateBatteryNotifications(
        batterySettings: BatteryNotificationsSettingsData
    ): UserDeviceRaw {
        val payload = PersonalBatteryNotificationsUpdatePayload(
            deviceId = appPrefs.deviceId,
            alertData = BaseRemoteModule.formatter.encodeToString(batterySettings)
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.ALERTS_BATTERY_UPDATE_SPACE,
            data = payload
        )

        if (response.isSuccess) {
            return UserDeviceRaw.from(response.responseDataMap!!)
        } else {
            throw response.remoteException
        }
    }
}