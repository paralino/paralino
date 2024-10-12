package app.paralino.android

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.os.Build
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.paralino.android.data.PreCheckControl
import app.paralino.android.di.KoinSetup
import app.paralino.android.tracking.LocationService
import app.paralino.core.CoreAppManager
import app.paralino.core.MR
import app.paralino.core.ParalinoCore
import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.notifications.IInfoMessagesNotificationPresenter
import app.paralino.core.notifications.IPushNotificationPresenter
import app.paralino.core.provider.CoreAppClass
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.LocationSharingManager
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import kotlin.coroutines.CoroutineContext

class AppClass : CoreAppClass(), ICoroutineScopeProvider, DefaultLifecycleObserver {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: AppClass
            private set
    }

    private val applicationJob = SupervisorJob()

    override val defaultScope = CoroutineScope(Dispatchers.Default + applicationJob)
    override val defaultIOScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override val userSessionIOScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override val defaultDispatcher: CoroutineContext get() = Dispatchers.Default
    override val ioDispatcher: CoroutineContext get() = Dispatchers.IO

    private val androidPreCheck: PreCheckControl by inject()
    private val coreAppManager: CoreAppManager by inject()
    private val locationSharingManager: LocationSharingManager by inject()
    private val infoMessagesNotifications: IInfoMessagesNotificationPresenter by inject()

    val isForeground get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    val appState get() = ProcessLifecycleOwner.get().lifecycle.currentState

    override fun onCreate() {
        super<CoreAppClass>.onCreate()
        init()
    }

    private fun init() {
        instance = this
        CoreAppClass.instance = this

        initCrashReporting()
        ParalinoCore.init(
            { androidContext(this@AppClass) },
            KoinSetup.modules
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        androidPreCheck.start()
        coreAppManager.onAppStart()
        locationSharingManager.sharingStatus
            .onEach {
                logg.v("Location sharing status changed: $it")
                if (it.data == true) {
                    logg.v("Location sharing contract ready, starting location service from state: $appState")
                    try {
                        LocationService.start(this, isForeground)
                    } catch (e: Exception) {
                        val message =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                                MR.strings.error_location_background_permission_missing_notification_message.getString(this)
                            } else {
                                MR.strings.error_location_sharing_start_notification_message.getString(this)
                            }
                        infoMessagesNotifications.notify(
                            IPushNotificationPresenter.Payload(
                                notificationId = 54343,
                                title = MR.strings.error_something_wrong.getString(this),
                                message = message,
                                timeMillis = null,
                                timeInfo = null
                            )
                        )
                    }
                }
            }
            .launchIn(defaultScope)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                gpsReceiver,
                IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION).apply { addAction(Intent.ACTION_PROVIDER_CHANGED) },
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                gpsReceiver,
                IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION).apply { addAction(Intent.ACTION_PROVIDER_CHANGED) }
            )
        }
        getSystemService<ConnectivityManager>()?.registerDefaultNetworkCallback(
            object : NetworkCallback() {
                override fun onAvailable(network: Network) {
                    coreAppManager.updateConnectionStatus()
                }

                override fun onLost(network: Network) {
                    coreAppManager.updateConnectionStatus()
                }
            }
        )
    }

    private fun initCrashReporting() {
        SentryAndroid.init(this) {
            it.isSendDefaultPii = false
            it.isEnabled = AppPrefs.instance.errorReporting
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        coreAppManager.onAppActive()
    }

    override fun onResume(owner: LifecycleOwner) {
        coreAppManager.onAppActive()
    }

    override fun onPause(owner: LifecycleOwner) {
        coreAppManager.onAppInactive()
    }

    override fun onStop(owner: LifecycleOwner) {
        coreAppManager.onAppBackground()
    }

    private val gpsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                coreAppManager.updateLocationStatus()
            }
        }
    }

    override fun onTerminate() {
        applicationJob.cancel()
        super.onTerminate()
    }
}