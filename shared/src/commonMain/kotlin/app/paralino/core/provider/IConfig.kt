package app.paralino.core.provider

interface IConfig {

    var deviceId: String
    var sessionId: String
    var deviceName: String
    var email: String
    var userId: String
    var emailConfirmed: Boolean
    var temporaryBackupPassword: String

    var pushTokenUnsynced: String

    var guestEmail: String
    var guestPassword: String

    var selectedGroupId: String?
    var dismissedAppVersion: Long

    val isTokenValid: Boolean
        get() = sessionId.isNotBlank()

    fun clear()
}