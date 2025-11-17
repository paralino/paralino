package app.paralino.core.crypto.user

import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.raw.UpdateUserKeysFunctionData
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.BaseRepository

class CryptoUserKeysRemoteSource(
    private val docApi: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val appConfig: AppConfigRepository,
): BaseRepository() {

    suspend fun getAllMyKeys(): List<UserCryptoKeyRaw> {
        return triggerRemoteOnly {
            docApi.listAllDocuments(
                databaseType = BackendDatabaseType.KEYS,
                collectionId = appConfig.resourceIds.collectionUserKeysID
            ).map { UserCryptoKeyRaw.from(it) }
        }
    }

    suspend fun createPersonalKey(
        data: UserCryptoKeyRaw,
        removeOldKeys: Boolean
    ): UserCryptoKeyRaw {
        return triggerRemoteOnly {
            val payload = UpdateUserKeysFunctionData(
                encryptedPublicKey = data.encryptedPublicKey,
                encryptedPrivateKey = data.encryptedPrivateKey,
                encryptedSymmetricKey = data.encryptedSymmetricKey,
                publicKey = data.publicKey,
                oldMasterKeys = data.oldMasterKeys,
                loginSalt = data.loginSalt,
                dataSalt = data.dataSalt,
                loginAlg = data.loginAlg,
                dataAlg = data.dataAlg,
                schemeVersion = data.schemeVersion,
                matchingKeyVersion = data.userKeyVersion,
                removeOldKeys = removeOldKeys,
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.KEYS_UPDATE_USER,
                data = payload
            )

            if (response.isSuccess) {
                UserCryptoKeyRaw.from(response.responseDataMap!!)
            } else {
                throw response.remoteException
            }
        }
    }
}