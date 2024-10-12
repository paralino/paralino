package app.paralino.core.repo.user

import app.paralino.core.db.AppDatabaseHelper
import app.paralino.core.data.db.UserConfigEntity
import app.paralino.core.db.mapToList
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.user.UserConfigModel
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class UserConfigRepository(
    private val database: AppDatabaseHelper,
    private val documentsApi: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val scopeProvider: ICoroutineScopeProvider,
) : BaseRepository() {

    val configState: StateFlow<UserConfigModel> = getConfig()
        .stateIn(
            scope = scopeProvider.defaultIOScope,
            started = SharingStarted.Eagerly,
            initialValue = UserConfigModel.fromDatabase(emptyList())
        )

    suspend fun refreshAndGetConfig(force: Boolean = false): UserConfigModel? {
        return triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "config_refresh",
                timeMillis = if (force) 0 else 120_000,
                failOnError = false
            )
        ) { getConfigFromRemote() }
            ?.also { config ->
                database.userConfigQueries.transaction {
                    database.userConfigQueries.clearAll()
                    config.nodes.forEach { node ->
                        database.userConfigQueries.createConfig(
                            UserConfigEntity(
                                id = node.id,
                                ownerUserId = node.ownerUserId,
                                type = node.type,
                                data_ = node.data
                            )
                        )
                    }
                }
            }
    }

    suspend fun ensureAndGetBillingUserId(): String {
        val billingCustomerId = configState.value.billingCustomerId

        if (!billingCustomerId.isNullOrBlank()) {
            return billingCustomerId
        }

        val freshCustomerId = refreshAndGetConfig(force = true)?.billingCustomerId

        if (!freshCustomerId.isNullOrBlank()) {
            return freshCustomerId
        }

        functionsApi.runAndWait(
            function = ServerFunction.BILLING_ID_UPDATE,
            data = null
        )

        return refreshAndGetConfig(force = true)?.billingCustomerId!!
    }

    private fun getConfig(): Flow<UserConfigModel> {
        return database.mapToList { userConfigQueries.getAll() }
            .map { UserConfigModel.fromDatabase(it) }
            .distinctUntilChanged()
    }

    private suspend fun getConfigFromRemote(): UserConfigModel {
        return blocking {
            val documents = documentsApi.listAllDocuments(
                databaseType = BackendDatabaseType.PRODUCT,
                collectionId = BackendResourceIDs.collectionUserConfigID
            )

            UserConfigModel.fromRemote(documents)
        }
    }
}