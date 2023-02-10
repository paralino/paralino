package app.paralino.core.repo.personal_code

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.mappers.PersonalCodeMapperCore
import app.paralino.core.models.raw.PersonalCodeFunctionData
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.raw.PersonalCodeResponse
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.time.Duration.Companion.days

class PersonalCodesRemoteSource constructor(
    private val functionsApi: IFunctionsApi,
    private val docApi: IDocumentsApi,
    private val remoteConfig: BackendConfig,
    private val personalCodeMapper: PersonalCodeMapperCore,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository
) {

    suspend fun tryCreatingCode(user: UserMe) {

        val personalCode = createPersonalCodeString()
        val derivedCode = cryptoCore.derivePassword(personalCode)
        val myKey = userKeyRepo.latestKeyPair()

        val secretKey = cryptoCore.getNewSymmetricKey()
        val secretHash = cryptoCore.hashProvider.computeHash(secretKey.content, HashType.SHA256)
            .asBase64String(cryptoCore.hashProvider)

        val secretKeyForOthers = cryptoCore.signData(
            data = secretHash,
            myPrivateKey = myKey.privateKey
        )
        val secretKeyForMe = cryptoCore.encryptDataWithKey(
            data = secretKey.asBase64String(cryptoCore.hashProvider),
            key = userKeyRepo.personalKey.content
        )

        val integrity = GroupIntegrity(
            timeUtc = nowUTC().toString(),
            signature = secretHash
        ).let {
            cryptoCore.encryptDataWithKeyPair(
                data = BaseRemoteModule.formatter.encodeToString(it),
                myPrivateKey = myKey.privateKey,
                publicKey = myKey.publicKey
            )
        }

        val codeData = UserPersonalCodeData(
            ownerId = user.id,
            ownerName = user.data?.userName ?: "",
            ownerImage = user.data?.userImage,
            ownerPublicKey = myKey.publicKey,
            code = personalCode,
            secretKey = secretKeyForOthers,
            integrity = integrity
        )

        val payload = PersonalCodeFunctionData(
            keyId = derivedCode.loginPassword,
            encryptedCodeData = cryptoCore.encryptDataWithSimplePassword(
                data = BaseRemoteModule.formatter.encodeToString(codeData),
                password = derivedCode.backupPassword
            ),
            encryptedCode = cryptoCore.encryptDataWithKey(
                data = codeData.code,
                key = userKeyRepo.personalKey.content
            ),
            verification = secretKeyForMe,
            validityUTC = null // nowUTC().plus(2.days).toString()
        )

        functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionPersonalCodeCreateID,
            data = BaseRemoteModule.formatter.encodeToString(payload)
        )
            .let {
                if (!it.isSuccess) {
                    throw BaseRemoteException.fromCode(
                        null,
                        "Unable to delete group",
                        Exception(it.stderr)
                    )
                }
            }
    }

    private fun createPersonalCodeString(): String {
        val key = cryptoCore.getNewSymmetricKey().asBase64String(cryptoCore.hashProvider)
        val derivedKey = cryptoCore.derivePassword(key).backupPassword

        // without similar characters: 0,O,1,I
        val code = derivedKey
            .replace(Regex("[^a-zA-Z0-9]"), "")
            .uppercase()
            .replace(Regex("[O01I]"), "")
            .take(8)

        return code
    }

    suspend fun getMyPersonalCodes(): List<PersonalCodeRaw> {
        return docApi.listAllDocuments(
            databaseType = BackendDatabaseType.DATA,
            collectionId = remoteConfig.resourceIds.collectionPersonalCodeID
        )
            .map { PersonalCodeRaw.from(it) }
            .mapNotNull {
                if (it.isStillValid()) {
                    it
                } else {
                    try {
                        revokePersonalCode(it.id)
                    } catch (e: Exception) {
                        logg.e("Error deleting expired personal code", e)
                    }
                    null
                }
            }
    }

    suspend fun getUserInfoFromCode(code: String): UserPersonalCodeData {
        val codeKeys = cryptoCore.derivePassword(code)
        return functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionPersonalCodeReadID,
            data = codeKeys.loginPassword
        )
            .takeIf { it.isSuccess }
            ?.also {
                if (it.response.isBlank())
                    throw NotFoundException("Code not found")
            }
            ?.let {
                val response =
                    BaseRemoteModule.formatter.decodeFromString<PersonalCodeResponse>(it.response)
                personalCodeMapper.remote2CoreRead(code, response.codeData).takeIf {
                    it.ownerId == response.userId
                }
            }!!
    }

    suspend fun revokePersonalCode(id: String) {
        docApi.deleteDocument(
            databaseType = BackendDatabaseType.DATA,
            remoteConfig.resourceIds.collectionPersonalCodeID,
            id
        )
    }

    suspend fun revokeAllCodes() {
        functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionPersonalCodeCreateID,
            data = ""
        )
            .let {
                if (!it.isSuccess) {
                    throw BaseRemoteException.fromCode(
                        null,
                        "Unable to delete group",
                        Exception(it.stderr)
                    )
                }
            }
    }

}