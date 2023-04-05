package app.paralino.core.provider

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.SqlDriverFactory
import app.paralino.core.models.BackendConfig
import app.paralino.core.notifications.IGroupMemberAddedNotificationsHandler
import app.paralino.core.notifications.IPlaceNotificationsHandler
import app.paralino.core.notifications.PlaceActivityNotificationPayload
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.IPushProviderManager
import org.koin.core.module.Module
import org.koin.dsl.module

object KoinIOS {

    fun createIOSKoinModules(
        deviceProvider: IDeviceApiProvider,
        connectionStatusProvider: IConnectionStatusProvider,
        locationStatusProvider: ILocationStatusProvider,
        pushSetupProvider: IPushProviderManager,
        placeNotificationHandler: IPlaceNotificationsHandler,
        groupActivityNotificationHandler: IGroupMemberAddedNotificationsHandler,
        geofenceManager: IGeofenceManager,
        backendConfig: BackendConfig
    ): List<Module> {
        return listOf(
            module {
                single(createdAtStart = true) { deviceProvider }
                single { connectionStatusProvider }
                single { locationStatusProvider }
                single { pushSetupProvider }
                single { placeNotificationHandler }
                single { groupActivityNotificationHandler }
                single { geofenceManager }
                single { backendConfig }
                single { CoroutineScopeProvider() }
                single { AppDatabaseHelper(SqlDriverFactory(), get()) }
            }
        )
    }

}