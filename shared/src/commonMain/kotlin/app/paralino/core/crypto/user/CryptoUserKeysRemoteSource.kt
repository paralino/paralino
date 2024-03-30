package app.paralino.core.crypto.user

import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.raw.UpdateUserKeysFunctionData
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.exceptions.BaseRemoteException

class CryptoUserKeysRemoteSource(
    private val docApi: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
) {

    suspend fun getAllMyKeys(): List<UserCryptoKeyRaw> {
        return docApi.listAllDocuments(
            databaseType = BackendDatabaseType.KEYS,
            collectionId = remoteConfig.resourceIds.collectionUserKeysID
        ).map { UserCryptoKeyRaw.from(it) }
    }

    suspend fun createPersonalKey(
        data: UserCryptoKeyRaw,
        removeOldKeys: Boolean
    ): UserCryptoKeyRaw {

        val payload = UpdateUserKeysFunctionData(
            encryptedPublicKey = data.encryptedPublicKey,
            encryptedPrivateKey = data.encryptedPrivateKey,
            encryptedSymmetricKey = data.encryptedSymmetricKey,
            keyVersion = data.version,
            removeOldKeys = removeOldKeys,
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.KEYS_UPDATE_USER,
            data = payload
        )

        if (response.isSuccess) {
            return UserCryptoKeyRaw.from(response.responseDataMap!!)
        } else {
            // TODO throw better exception
            throw BaseRemoteException.fromCode(
                code = response.statusCode,
                message = "Unable to update user key",
                cause = Exception(response.stderr)
            )
        }
    }
}