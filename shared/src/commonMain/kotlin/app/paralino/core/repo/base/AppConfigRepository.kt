package app.paralino.core.repo.base

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.ConfigNodeEntity
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.ConfigNode
import app.paralino.core.models.base.DeviceType
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.data.db.AppConfig
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AppConfigRepository(
    private val database: AppDatabaseHelper,
    private val api: IDocumentsApi,
    private val deviceApiProvider: IDeviceApiProvider,
    private val config: AppConfig
) : BaseRepository() {


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
                    nodes.forEach { node ->
                        database.configQueries.createConfig(
                            ConfigNodeEntity(id = node.id, value_ = node.value)
                        )
                    }
                }
            }
    }

    suspend fun cancelNewAppVersion() {
        val appConfig = getConfigOnce() ?: return
        config.dismissedAppVersion = when (deviceApiProvider.deviceType) {
            DeviceType.Android -> appConfig.newVersionAndroid
            DeviceType.iOS -> appConfig.newVersionIOS
        }
    }

    private fun getConfig(): Flow<ConfigData> {
        return database.mapToList { configQueries.getAll() }
            .map { it.map { ConfigNode(id = it.id, value = it.value_ ?: "") } }
            .map { ConfigData.init(it) }
            .distinctUntilChanged()
    }

    private suspend fun getConfigOnce(): ConfigData? {
        return database.configQueries
            .getAll()
            .executeAsList()
            .map { ConfigNode(id = it.id, value = it.value_ ?: "") }
            .let { ConfigData.init(it) }
    }

    private suspend fun getConfigFromRemote(): List<ConfigNode> {
        return blocking {
            api.listAllDocuments(
                databaseType = BackendDatabaseType.PRODUCT,
                collectionId = BackendResourceIDs.collectionConfigID
            ).map { ConfigNode.from(it) }
        }
    }

    private fun getUpdateType(configData: ConfigData): AppUpdateType {
        val currentVersion = deviceApiProvider.appVersionCode
        val minVersion = when (deviceApiProvider.deviceType) {
            DeviceType.Android -> configData.minVersionAndroid
            DeviceType.iOS -> configData.minVersionIOS
        }
        val newVersion = when (deviceApiProvider.deviceType) {
            DeviceType.Android -> configData.newVersionAndroid
            DeviceType.iOS -> configData.newVersionIOS
        }

        return when {
            currentVersion < minVersion -> AppUpdateType.FORCE
            currentVersion < newVersion && newVersion > config.dismissedAppVersion -> AppUpdateType.SOFT
            else -> AppUpdateType.NONE
        }
    }
}

enum class AppUpdateType {
    NONE, SOFT, FORCE
}