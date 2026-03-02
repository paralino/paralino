package app.paralino.android.notification

import app.paralino.android.provider.IUnifiedPushManager
import app.paralino.android.provider.UnifiedPushError
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.main.user.UnifiedPushEndpointConfig
import app.paralino.core.logg
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.koin.android.ext.android.inject
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

class UnifiedPushEventService: PushService() {

    private val serviceDefaultScope = CoroutineScope(Dispatchers.Default + Job())

    private val pushProvider: IPushProviderManager by inject()
    private val serviceManager: NotificationSocketServiceManager by inject()
    private val appPrefs: AppPrefs by inject()
    private val unifiedPushManager: IUnifiedPushManager by inject()


    override fun onNewEndpoint(
        endpoint: PushEndpoint,
        instance: String
    ) {
        logg.tag("push_notifications").i("Got new unifiedPush endpoint for ${appPrefs.selectedUnifiedPushDistributor}")
        appPrefs.unifiedPushEndpointConfigUnsynced = try {
            appPrefs.selectedUnifiedPushDistributor?.let {
                BaseRemoteModule.formatter.encodeToString(
                    UnifiedPushEndpointConfig(
                        distributor = it,
                        url = endpoint.url,
                        pubkey = endpoint.pubKeySet?.pubKey,
                        auth = endpoint.pubKeySet?.auth
                    )
                )
            }
        } catch (e: Exception) {
            null
        }
        pushProvider.syncPushToken()
    }

    override fun onMessage(
        message: PushMessage,
        instance: String
    ) {
        logg.tag("push_notifications").d("Got new unifiedPush push notification")
        pushProvider.syncPushNotifications()
    }

    override fun onRegistrationFailed(
        reason: FailedReason,
        instance: String
    ) {
        logg.tag("push_notifications").i("Got unifiedPush registration failed $reason for ${appPrefs.selectedUnifiedPushDistributor}")
        unifiedPushManager.emitError(UnifiedPushError.RegistrationFailed(reason.toString()))
    }

    override fun onUnregistered(instance: String) {
        logg.tag("push_notifications").i("UnifiedPush unregistered for ${appPrefs.selectedUnifiedPushDistributor}")
        appPrefs.unifiedPushEndpointConfigUnsynced = ""
        appPrefs.selectedUnifiedPushDistributor = null
        pushProvider.syncPushToken()
    }

    override fun onTempUnavailable(instance: String) {
        logg.tag("push_notifications").i("UnifiedPush temporary unavailable for ${appPrefs.selectedUnifiedPushDistributor}")
        super.onTempUnavailable(instance)
    }
}