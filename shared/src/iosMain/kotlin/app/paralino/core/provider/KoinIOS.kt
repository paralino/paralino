package app.paralino.core.provider

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.SqlDriverFactory
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.BackendConfig
import app.paralino.core.notifications.IGroupMemberAddedNotificationsHandler
import app.paralino.core.notifications.IPlaceNotificationsHandler
import app.paralino.core.repo.api.*
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
        backendConfig: BackendConfig,
        remoteModule: (sessionMonitor: UserSessionMonitor) -> IRemoteModule,
        documentsApi: (IRemoteModule, BackendConfig) -> IDocumentsApi,
        userApi: (IRemoteModule, BackendConfig) -> IUserApi,
        functionsApi: (IRemoteModule, BackendConfig) -> IFunctionsApi,
        realtimeApi: (IRemoteModule, BackendConfig) -> IRealtimeApiIOS,
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
                single { remoteModule(get()) }
                single { backendConfig }
                single { documentsApi(get(), get()) }
                single { userApi(get(), get()) }
                single { functionsApi(get(), get()) }
                single { RealtimeApiWrapperIOS(realtimeApi(get(), get())) }
                single { CoroutineScopeProvider() }
                single { AppDatabaseHelper(SqlDriverFactory(), get()) }
            }
        )
    }

}