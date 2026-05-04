package app.paralino.core.domain.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.groups.BatteryAlertEventEntity
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.group.BatteryNotificationsSettingsData
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.raw.GroupBatteryAlertPayload
import app.paralino.core.models.raw.PersonalBatteryAlertPayload
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAsync
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

class BatteryAlertUploadEngine(
    private val functionsApi: IFunctionsApi,
    private val appDatabase: MainDatabaseHelper,
    private val locationSharingManager: LocationSharingManager,
    private val scopeProvider: ICoroutineScopeProvider,
    private val appPrefs: AppPrefs
) : LimitedExecutionRepository() {

    private val executionKey = "batteryAlerts"

    fun sendBatteryAlert(
        payloads: List<MemberGroupId>
    ) {
        scopeProvider.userSessionIOScope.launch {
            payloads.forEach { payload ->
                sendBatteryAlert(
                    groupId = payload.groupId,
                    memberId = payload.memberId,
                    locationId = payload.locationId,
                    batteryLevel = payload.deviceStatus?.batteryLevel
                )
            }
        }
    }

    private suspend fun sendBatteryAlert(
        groupId: GroupOrSpaceId,
        memberId: String,
        locationId: String,
        batteryLevel: Int?
    ) {
        val executionKey = "${executionKey}_${groupId}"
        try {
            startProcessing(executionKey)
        } catch (e: Exception) {
            logg.tag(executionKey).v("Processing, can't sent battery event")
            return
        }

        try {

            if (batteryLevel == null) {
                return
            }

            if (batteryLevel >= BatteryNotificationsSettingsData.LOW_BATTERY_LEVEL) {
                clearBatteryAlert(groupId = groupId)
                return
            }

            val contract = locationSharingManager.currentSharingContractForGroup(id = groupId)
            val isContractValid = contract?.asValidContract()?.shareBattery == true
            val isBatteryStateValid = batteryLevel < BatteryNotificationsSettingsData.LOW_BATTERY_LEVEL

            logg.tag(executionKey).v("Battery alert contract valid: $isContractValid, stateValid: $isBatteryStateValid")

            if (!isContractValid || !isBatteryStateValid) {
                return
            }

            // state is valid, we can send event
            val currentEvent = appDatabase.groupRelatedDao.getBatteryAlertEventOnce(groupId = groupId.id)

            if (
                currentEvent == null
                || currentEvent.notifyTimeMillis < nowUTC().minus(4.hours).toEpochMilliseconds()
            ) {
                sendBatteryAlertImpl(
                    groupId = groupId,
                    memberId = memberId,
                    locationId = locationId
                )
                return
            } else {
                return
            }
        } catch (e: Exception) {
            return
        } finally {
            stopProcessing(executionKey)
        }
    }

    private suspend fun clearBatteryAlert(groupId: GroupOrSpaceId) {
        appDatabase.groupRelatedDao.deleteBatteryAlertEvent(groupId = groupId.id)
    }

    private suspend fun sendBatteryAlertImpl(
        groupId: GroupOrSpaceId,
        memberId: String,
        locationId: String
    ) {
        try {
            blocking {
                logg.tag(executionKey).v("Sending battery alert event")
                if (groupId.isPersonalSpace) {
                    functionsApi.runAsync(
                        function = ServerFunction.EVENT_BATTERY_SPACE,
                        data = PersonalBatteryAlertPayload(
                            deviceId = appPrefs.deviceId
                        )
                    )
                } else {
                    functionsApi.runAsync(
                        function = ServerFunction.EVENT_BATTERY_GROUP,
                        data = GroupBatteryAlertPayload(
                            groupId = groupId.id,
                            memberId = memberId,
                            locationId = locationId
                        )
                    )
                }
            }
            appDatabase.groupRelatedDao.createBatteryAlertEvent(
                BatteryAlertEventEntity(
                    groupId = groupId.id,
                    notifyTimeMillis = nowUTC().toEpochMilliseconds()
                )
            )
        } catch (e: Exception) {
            logg.e("Error sending battery alert event", e)
        }
    }
}