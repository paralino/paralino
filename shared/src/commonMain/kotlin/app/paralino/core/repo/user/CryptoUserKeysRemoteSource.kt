package app.paralino.core.repo.user

import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.api.IDocumentsApi

class CryptoUserKeysRemoteSource(
    private val docApi: IDocumentsApi,
    private val remoteConfig: BackendConfig,
) {

    suspend fun getAllMyKeys(): List<UserCryptoKeyRaw> {
        return docApi.getDocuments(databaseType = BackendDatabaseType.KEYS, remoteConfig.resourceIds.collectionUserKeysID)
            .map { UserCryptoKeyRaw.from(it) }
    }

    suspend fun createPersonalKey(data: UserCryptoKeyRaw): UserCryptoKeyRaw {
        // TODO review: move to separate function or restrict permission + adjust database index to be unique
        return docApi.createDocument(
            databaseType = BackendDatabaseType.KEYS,
            collectionId = remoteConfig.resourceIds.collectionUserKeysID,
            data = data.toPostJson()
        ).let {
            UserCryptoKeyRaw.from(it)
        }
    }

    suspend fun deleteUserKey(keyId: String) {
        docApi.deleteDocument(
            databaseType = BackendDatabaseType.KEYS,
            collectionId = remoteConfig.resourceIds.collectionUserKeysID,
            documentId = keyId
        )
    }

}