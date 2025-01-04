package app.paralino.core.repo.base

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.common.RemoteConfigEntity
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.DeviceType
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration.Companion.minutes

class RemoteConfigRepository(
    private val database: MainDatabaseHelper,
    private val functionsApi: IFunctionsApi,
    private val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs,
) : BaseRepository() {

    private val cachedConfigState = MutableStateFlow<ConfigData?>(null)

    fun getUpdateType(): Flow<AppUpdateType> {
        return getConfigLocally()
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
        ) { getAndUpdateConfigFromRemote() }
    }

    suspend fun cancelNewAppVersion() {
        val remoteConfig = getConfigOnce() ?: return
        appPrefs.dismissedAppVersion = when (deviceApiProvider.deviceType) {
            DeviceType.Android -> remoteConfig.newVersionAndroid
            DeviceType.iOS -> remoteConfig.newVersionIOS
        }
    }

    fun getConfigLocally(): Flow<ConfigData> {
        return database.remoteConfigDao.getAll()
            .map { ConfigData.init(it) }
            .onEach { newValue -> cachedConfigState.update { newValue } }
            .distinctUntilChanged()
    }

    fun getCachedConfigOnce(): ConfigData {
        return cachedConfigState.value ?: ConfigData.init(emptyList())
    }

    suspend fun getConfigOnce(): ConfigData {
        return cachedConfigState.value
            ?: getAndUpdateConfigFromRemote()
            ?: ConfigData.init(database.remoteConfigDao.getAllOnce())
    }

    private suspend fun getAndUpdateConfigFromRemote(): ConfigData? {
        val response = functionsApi.runAndWait(function = ServerFunction.CONFIG)
        val jsonObject = response.responseData?.jsonObject

        if (jsonObject.isNullOrEmpty()) return null

        val newConfigValues = jsonObject.entries.map { node ->
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

        val newConfig = ConfigData.init(newConfigValues)
        cachedConfigState.update { newConfig }

        database.remoteConfigDao.recreateConfig(newConfigValues)

        return newConfig
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