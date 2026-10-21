package app.paralino.core.repo.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.domain.user.GetMapSDKProviderUseCase
import app.paralino.core.models.raw.UserConfigPayload
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.models.user.UserConfigModel
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class UserConfigRepository(
    private val database: MainDatabaseHelper,
    private val functionsApi: IFunctionsApi,
    private val scopeProvider: ICoroutineScopeProvider,
    private val appPrefs: AppPrefs,
    private val getMapSDKProvider: GetMapSDKProviderUseCase
) : BaseRepository() {

    val configState: StateFlow<UserConfigModel> = getConfig()
        .stateIn(
            scope = scopeProvider.defaultIOScope,
            started = SharingStarted.Eagerly,
            initialValue = UserConfigModel.fromDatabase(
                nodes = emptyList(),
                mapTilerStyleToken = null,
                mapTilerSatelliteToken = null
            )
        )

    suspend fun refreshMaybeAndGetConfig(
        cacheTime: Duration?
    ): UserConfigModel? {
        if (cacheTime == null) {
            configState.value.takeIf {
                val isTokenStateCorrect = appPrefs.mapTilerTokenV1.isNullOrBlank()
                it.isValid && isTokenStateCorrect
            }?.let { config ->
                return config
            }
        }

        return triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "user_config_refresh",
                timeMillis = cacheTime?.inWholeMilliseconds ?: 0,
                failOnError = false
            )
        ) {
            getConfigFromRemote()
        }
            ?.also { config ->
                database.userConfigDao.recreateConfig(config.toDatabaseEntities())
                appPrefs.mapTilerTokenV1 = null
                appPrefs.mapTilerStylesToken = config.mapTilerStyleToken
                appPrefs.mapTilerSatelliteToken = config.mapTilerSatelliteToken
            }
    }

    suspend fun ensureAndGetBillingUserId(): String {
        val billingCustomerId = configState.value.billingCustomerId

        if (!billingCustomerId.isNullOrBlank()) {
            return billingCustomerId
        }

        val freshCustomerId = refreshMaybeAndGetConfig(cacheTime = 0.seconds)?.billingCustomerId

        if (!freshCustomerId.isNullOrBlank()) {
            return freshCustomerId
        }

        functionsApi.runAndWait(
            function = ServerFunction.BILLING_ID_UPDATE,
            data = null
        )

        return refreshMaybeAndGetConfig(cacheTime = 0.seconds)?.billingCustomerId!!
    }

    private fun getConfig(): Flow<UserConfigModel> {
        return database.observe { userConfigDao.getAll() }
            .map {
                UserConfigModel.fromDatabase(
                    nodes = it,
                    mapTilerStyleToken = appPrefs.mapTilerStylesToken,
                    mapTilerSatelliteToken = appPrefs.mapTilerSatelliteToken
                )
            }
            .distinctUntilChanged()
    }

    private suspend fun getConfigFromRemote(): UserConfigModel {
        return blocking {
            val premiumMapProvider = getMapSDKProvider.getProvider() as? MapsSDKProvider.MAPLIBRE.MAPLIBRE_PREMIUM
            val payload = UserConfigPayload(
                satelliteMaps = premiumMapProvider != null,
                premiumMapStyles = premiumMapProvider != null &&
                        (premiumMapProvider.lightStyle.isPremium || premiumMapProvider.darkStyle.isPremium)
            )

            val execution = functionsApi.runAndWait(
                function = ServerFunction.USER_CONFIG,
                data = payload
            )

            if (!execution.isSuccess) {
                throw execution.remoteException
            }

            val data = execution.responseDataMap
            UserConfigModel.fromRemote(data)
        }
    }
}