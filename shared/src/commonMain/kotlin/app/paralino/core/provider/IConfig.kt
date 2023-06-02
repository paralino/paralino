package app.paralino.core.provider

abstract class IConfig {

    companion object {
        const val keyToken = "prefs.profile.token"
        const val keyUserKey = "prefs.user.masterKey"
        const val keySessionKey = "prefs.user.sessionKey"
        const val keyEmail = "prefs.profile.email"
        const val keyUserId = "prefs.profile.user_id"
        const val keyTempPassword = "prefs.user.temp_password"
        const val keyEmailConfirmed = "prefs.profile.email.confirmed"
        const val keyEmailGuest = "prefs.profile.guest.email"
        const val keyPasswordGuest = "prefs.profile.guest.password"
        const val keyPushTokenUnsynced = "prefs.device.pushToken.unsynced"
        const val keyPushTokenSynced = "prefs.device.pushToken.synced"
        const val keyDeviceID = "prefs.device.ID"
        const val keyDeviceIDSeed = "prefs.device.ID.seed"
        const val keySelectedGroupID = "prefs.group.selectedGroupID"
        const val keyDismissedAppVersionID = "prefs.device.dismissedAppVersion"
    }

    abstract var encryptedUserMasterKey: String
    abstract var sessionMasterKey: String

    abstract var deviceIdSeed: String
    abstract var deviceId: String
    abstract var sessionId: String
    abstract var email: String
    abstract var userId: String
    abstract var emailConfirmed: Boolean
    abstract var temporaryBackupPassword: String

    abstract var pushTokenUnsynced: String
    abstract var pushTokenSynced: String

    abstract var guestEmail: String
    abstract var guestPassword: String

    abstract var selectedGroupId: String?
    abstract var dismissedAppVersion: Long

    val isTokenValid: Boolean
        get() = sessionId.isNotBlank()

    abstract fun clear()
}