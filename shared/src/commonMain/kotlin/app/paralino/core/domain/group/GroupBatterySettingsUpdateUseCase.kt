package app.paralino.core.domain.group

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.models.group.BatteryNotificationsSettingsData
import app.paralino.core.models.group.BatteryNotificationsSettingsEntry
import app.paralino.core.models.group.UserGroup
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.location.LocationLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GroupBatterySettingsUpdateUseCase(
    private val remote: GroupsRemoteSource,
    private val memberLocal: LocationLocalDataSource,
    private val appPrefs: AppPrefs
): LimitedExecutionRepository() {

    fun updateBatterySettings(
        group: UserGroup,
        active: Boolean
    ): Flow<Unit> {
        return getDataOnce(
            remote = {
                val member = memberLocal.getMemberOnce(group = group, userId = appPrefs.userId)!!
                val currentSettings = member.settings?.batteryAlertSettings

                val settings = if (currentSettings != null) {
                    currentSettings.copy(isActive = active)
                } else {
                    BatteryNotificationsSettingsData(
                        isActive = active,
                        settings = emptyList()
                    )
                }
                remote.updateBatteryNotifications(
                    groupId = group.id,
                    memberId = member.id,
                    batterySettings = settings
                )
            },
            localUpdate = {
                memberLocal.updateMemberBatterySettings(data = it)
            }
        ).map { Unit }
    }

    fun updateBatterySettings(
        group: UserGroup,
        data: BatteryNotificationsSettingsEntry
    ): Flow<Unit> {
        return getDataOnce(
            remote = {
                val member = memberLocal.getMemberOnce(group = group, userId = appPrefs.userId)!!
                val currentSettings = member.settings?.batteryAlertSettings

                val settings = if (currentSettings != null) {
                    val currentAlert =
                        currentSettings.settings.firstOrNull { it.memberUserId == data.memberUserId }

                    val newAlerts = if (currentAlert != null) {
                        currentSettings.settings.toMutableList().apply {
                            removeAll { it.memberUserId == data.memberUserId }
                            add(data)
                        }
                    } else {
                        currentSettings.settings.toMutableList().apply { add(data) }
                    }

                    currentSettings.copy(settings = newAlerts)
                } else {
                    BatteryNotificationsSettingsData(
                        settings = listOf(data)
                    )
                }
                remote.updateBatteryNotifications(
                    groupId = group.id,
                    memberId = member.id,
                    batterySettings = settings
                )
            },
            localUpdate = {
                memberLocal.updateMemberBatterySettings(data = it)
            }
        ).map { Unit }
    }
}