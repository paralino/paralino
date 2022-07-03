package app.paralino.core.repo.user

import app.paralino.core.models.BackendConfig
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.api.IDocumentsApi

class CryptoUserKeysRemoteSource(
    private val docApi: IDocumentsApi,
    private val remoteConfig: BackendConfig,
) {

    suspend fun getAllMyKeys(): List<UserCryptoKeyRaw> {
        return docApi.getDocuments(remoteConfig.resourceIds.collectionUserKeysID)
            .map { UserCryptoKeyRaw.from(it) }
    }

    suspend fun createPersonalKey(data: UserCryptoKeyRaw): UserCryptoKeyRaw {
        return docApi.createOrUpdateDocument(
            collectionId = remoteConfig.resourceIds.collectionUserKeysID,
            docId = null,
            data = data.toPostJson()
        ).let {
            UserCryptoKeyRaw.from(it)
        }
    }

    suspend fun deleteUserKey(keyId: String) {
        docApi.deleteDocument(
            collectionId = remoteConfig.resourceIds.collectionUserKeysID,
            documentId = keyId
        )
    }

}