package app.paralino.core.crypto.user

import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.raw.UpdateUserKeysFunctionData
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait

class CryptoUserKeysRemoteSource(
    private val docApi: IDocumentsApi,
    private val functionsApi: IFunctionsApi,
    private val appBuildConfig: AppBuildConfig,
) {

    suspend fun getAllMyKeys(): List<UserCryptoKeyRaw> {
        return docApi.listAllDocuments(
            databaseType = BackendDatabaseType.KEYS,
            collectionId = appBuildConfig.resourceIds.collectionUserKeysID
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
            publicKey = data.publicKey,
            oldMasterKeys = data.oldMasterKeys,
            loginSalt = data.loginSalt,
            loginAlg = data.loginAlg,
            dataAlg = data.dataAlg,
            matchingKeyVersion = data.userKeyVersion,
            removeOldKeys = removeOldKeys,
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.KEYS_UPDATE_USER,
            data = payload
        )

        if (response.isSuccess) {
            return UserCryptoKeyRaw.from(response.responseDataMap!!)
        } else {
            throw response.remoteException
        }
    }
}