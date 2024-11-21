package app.paralino.core.repo.base

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.main.common.RemoteConfigEntity
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.minutes

class RemoteConfigRepository(
    private val database: MainDatabaseHelper,
    private val functionsApi: IFunctionsApi,
    private val deviceApiProvider: IDeviceApiProvider,
    private val scopeProvider: ICoroutineScopeProvider,
    private val appPrefs: AppPrefs,
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

    suspend fun refreshConfig(cacheTimeMillis: Long = 1.minutes.inWholeMilliseconds) {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "config_refresh",
                timeMillis = cacheTimeMillis,
                failOnError = false
            )
        ) { getConfigFromRemote() }
            ?.also { nodes ->
                database.remoteConfigDao.recreateConfig(
                    nodes.entries.map { node ->
                        val value = node.value
                        RemoteConfigEntity(
                            id = node.key,
                            value = if (value is JsonPrimitive && value.isString) {
                                value.content
                            } else {
                                value.toString()
                            }
                        )
                    }
                )
            }
    }

    suspend fun cancelNewAppVersion() {
        val remoteConfig = configState.value
        appPrefs.dismissedAppVersion = when (deviceApiProvider.deviceType) {
            DeviceType.Android -> remoteConfig.newVersionAndroid
            DeviceType.iOS -> remoteConfig.newVersionIOS
        }
    }

    fun getConfig(): Flow<ConfigData> {
        return database.remoteConfigDao.getAll()
            .map { ConfigData.init(it) }
            .distinctUntilChanged()
    }

    fun getConfigOnce(): ConfigData {
        return configState.value
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
            currentVersion < newVersion && newVersion > appPrefs.dismissedAppVersion -> AppUpdateType.SOFT(
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