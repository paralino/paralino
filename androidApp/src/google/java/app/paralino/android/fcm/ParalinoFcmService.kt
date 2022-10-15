package app.paralino.android.fcm

import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.repo.user.UserRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import loge
import logv
import org.koin.android.ext.android.inject

class ParalinoFcmService: FirebaseMessagingService() {

    val userRepository: UserRepository by inject()
    val scopeProvider: ICoroutineScopeProvider by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        logv("push_notifications", "Got new token: $token")
        scopeProvider.userSessionIOScope.launch {
            try {
                userRepository.registerPushToken(token)
            } catch (e: Exception) {
                loge("push_notifications", "Error registering push token", e)
            }
        }

    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
    }

}