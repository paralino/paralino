package app.paralino.core.utils

interface IConfig {

    var sessionId: String
    var deviceName: String
    var email: String
    var userId: String
    var emailConfirmed: Boolean
    var temporaryBackupPassword: String

    var pushTokenUnsynced: String

    var guestEmail: String
    var guestPassword: String

    val isTokenValid: Boolean
        get() = sessionId.isNotBlank()

    fun clear()
}