package com.zbapp.core.repo.user

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.raw.UserCryptoKeyRaw
import com.zbapp.core.repo.api.IDocumentsApi

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