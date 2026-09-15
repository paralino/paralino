package app.paralino.core.db.main.groups

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memberEntity")
data class MemberEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "createdAt") val createdAt: String?,
    @ColumnInfo(name = "userId") val userId: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "groupKeyVersion") val groupKeyVersion: Long,
    @ColumnInfo(name = "verification") val verification: String,
    @ColumnInfo(name = "data") val data: String,
    @ColumnInfo(name = "settings_id") val settingsId: String?,
    @ColumnInfo(name = "settings_ownerUserId") val settingsOwnerUserId: String?,
    @ColumnInfo(name = "settings_groupId") val settingsGroupId: String?,
    @ColumnInfo(name = "settings_memberIntegrity") val settingsMemberIntegrity: String?,
    @ColumnInfo(name = "settings_groupIntegrity") val settingsGroupIntegrity: String?,
    @ColumnInfo(name = "settings_memberIntegrityUserKeyVersion") val settingsMemberIntegrityUserKeyVersion: Long?,
    @ColumnInfo(name = "settings_groupIntegrityUserKeyVersion") val settingsGroupIntegrityUserKeyVersion: Long?,
    @ColumnInfo(name = "settings_batteryAlertSettings") val settingsBatteryAlertSettings: String?,
    @ColumnInfo(name = "paralinoId") val paralinoId: String?,
    @ColumnInfo(name = "memberKeyVersion") val memberKeyVersion: Long
)

@Entity
data class MemberEntityBatterySettingsUpdate(
    @ColumnInfo(name = "id") var id: String,
    @ColumnInfo(name = "settings_batteryAlertSettings") val settingsBatteryAlertSettings: String?
)
