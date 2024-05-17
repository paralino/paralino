package app.paralino.core.repo.base

import app.paralino.core.data.db.AppConfig
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.ConfigNodeEntity
import app.paralino.core.data.db.mapToList
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.DeviceType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class AppConfigRepository(
    private val database: AppDatabaseHelper,
    private val functionsApi: IFunctionsApi,
    private val deviceApiProvider: IDeviceApiProvider,
    private val scopeProvider: ICoroutineScopeProvider,
    private val config: AppConfig,
) : BaseRepository() {

    val configState: StateFlow<ConfigData> = getConfig()
        .stateIn(
            scope = scopeProvider.defaultIOScope,
            started = SharingStarted.Eagerly,
            initialValue = ConfigData.init(emptyList())
        )

    fun getUpdateType(): Flow<AppUpdateType> {
        return getConfig()
            .map { getUpdateType(it) }
            .distinctUntilChanged()
    }

    suspend fun refreshConfig() {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "config_refresh",
                timeMillis = 10_000,
                failOnError = false
            )
        ) { getConfigFromRemote() }
            ?.also { nodes ->
                database.configQueries.transaction {
                    database.configQueries.clearAll()
                    nodes.entries.forEach { node ->
                        database.configQueries.createConfig(
                            ConfigNodeEntity(id = node.key, value_ = node.value.toString())
                        )
                    }
                }
            }
    }

    suspend fun cancelNewAppVersion() {
        val appConfig = getConfigOnce()
        config.dismissedAppVersion = when (deviceApiProvider.deviceType) {
            DeviceType.Android -> appConfig.newVersionAndroid
            DeviceType.iOS -> appConfig.newVersionIOS
        }
    }

    fun getConfig(): Flow<ConfigData> {
        return database.mapToList { configQueries.getAll() }
            .map { ConfigData.init(it) }
            .distinctUntilChanged()
    }

    private suspend fun getConfigOnce(): ConfigData {
        return database.configQueries
            .getAll()
            .executeAsList()
            .let { ConfigData.init(it) }
    }

    private suspend fun getConfigFromRemote(): JsonObject? {
        return blocking {
            val response = functionsApi.runAndWait(function = ServerFunction.CONFIG)
            response.responseData?.jsonObject
        }
    }

    private fun getUpdateType(configData: ConfigData): AppUpdateType {
        val currentVersion = deviceApiProvider.appVersionCode
        val minVersion = when (deviceApiProvider.deviceType) {
            DeviceType.Android -> configData.minVersionAndroid
            DeviceType.iOS -> configData.minVersionIOS
        }
        val minUiVersion = when (deviceApiProvider.deviceType) {
            DeviceType.Android -> configData.minUiVersionAndroid
            DeviceType.iOS -> configData.minUiVersionIOS
        }
        val newVersion = when (deviceApiProvider.deviceType) {
            DeviceType.Android -> configData.newVersionAndroid
            DeviceType.iOS -> configData.newVersionIOS
        }

        return when {
            currentVersion < minVersion -> AppUpdateType.FORCE_AND_CANCEL(configData)
            currentVersion < minUiVersion -> AppUpdateType.FORCE(configData)
            currentVersion < newVersion && newVersion > config.dismissedAppVersion -> AppUpdateType.SOFT(
                configData
            )

            else -> AppUpdateType.NONE(configData)
        }
    }
}

sealed interface AppUpdateType {
    val configData: ConfigData

    data class NONE(override val configData: ConfigData) : AppUpdateType
    data class SOFT(override val configData: ConfigData) : AppUpdateType
    data class FORCE(override val configData: ConfigData) : AppUpdateType
    data class FORCE_AND_CANCEL(override val configData: ConfigData) : AppUpdateType

    val isForced: Boolean
        get() = when (this) {
            is FORCE,
            is FORCE_AND_CANCEL -> true

            is NONE,
            is SOFT -> false
        }
}