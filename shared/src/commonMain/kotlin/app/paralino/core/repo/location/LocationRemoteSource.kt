package app.paralino.core.repo.location

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.exceptions.MissingMemberKeyException
import app.paralino.core.utils.extensions.blocking
import kotlinx.serialization.encodeToString

class LocationRemoteSource constructor(
    private val documentApi: IDocumentsApi,
    private val remoteConfig: BackendConfig,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKRepository,
) {

    suspend fun deleteLocation(id: String) {
        blocking {
            documentApi.deleteDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionMemberLocationsID,
                documentId = id
            )
        }
    }

    suspend fun clearLocation(id: String) {
        blocking {
            documentApi.updateDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = remoteConfig.resourceIds.collectionMemberLocationsID,
                docId = id,
                data = mapOf(
                    "locationData" to "",
                )
            )
        }
    }

    suspend fun saveCurrentLocation(
        ids: List<MemberGroupId>,
    ) {
        blocking {

            ids.forEach {

                val memberKey = cryptoKEKRepo.getMemberKey(it.groupId, config.userId)
                    ?: throw MissingMemberKeyException(it.groupId, config.userId)

                val groupKey = cryptoKEKRepo.getGroupKey(it.groupId)
                    ?: throw MissingGroupKeyException(it.groupId)

                val encryptedData = it.data?.let {
                    cryptoCore.encryptDataWithKey(
                        data = BaseRemoteModule.formatter.encodeToString(it),
                        key = memberKey.data.content
                    )
                }

                documentApi.updateDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = remoteConfig.resourceIds.collectionMemberLocationsID,
                    docId = it.locationId,
                    data = mapOf(
                        "locationData" to encryptedData,
                        "deviceData" to ""
                    )
                )
            }

        }
    }

}