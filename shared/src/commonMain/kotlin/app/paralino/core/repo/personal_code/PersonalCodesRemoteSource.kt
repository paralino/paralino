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
import app.paralino.core.models.user.PersonalCodeKeyData
import app.paralino.core.models.user.PersonalCodeUserData
import app.paralino.core.models.user.SimpleKeyPairData
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserMeData
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class PersonalCodesRemoteSource constructor(
    private val functionsApi: IFunctionsApi,
    private val docApi: IDocumentsApi,
    private val remoteConfig: BackendConfig,
    private val personalCodeMapper: PersonalCodeMapperCore,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository
) {

    suspend fun tryCreatingCode(user: UserMe, userMeData: UserMeData?) {

        val personalCode = createPersonalCodeString()
        val derivedCode = cryptoCore.derivePassword(personalCode)
        val myKey = userKeyRepo.latestKeyPair()

        val exchangeKeyPair = cryptoCore.getNewCryptoKeyPair()
        val secretHash =
            cryptoCore.hashProvider.computeHash(exchangeKeyPair.publicKey, HashType.SHA256)

        val memberVerificationSecret = cryptoCore.signData(
            data = secretHash,
            myPrivateKey = myKey.privateKey
        )

        val integrity = GroupIntegrity(
            timeUtc = nowUTC().toString(),
            // this signature will always be invalid, but we are passing some for consistency
            // this also ensures user will always rotate keys when added to new group
            signature = secretHash
        ).let {
            cryptoCore.encryptDataWithKeyPair(
                data = BaseRemoteModule.formatter.encodeToString(it),
                myPrivateKey = exchangeKeyPair.privateKey,
                publicKey = exchangeKeyPair.publicKey
            )
        }.let {
            cryptoCore.signData(data = it, myPrivateKey = myKey.privateKey)
        }

        val codeUserData = PersonalCodeUserData(
            ownerUserId = user.id,
            ownerName = (userMeData ?: user.data)?.userName ?: "",
            ownerImage = (userMeData ?: user.data)?.userImage,
            ownerIdentityKey = myKey.publicKey,
        )

        val codeKeyData = PersonalCodeKeyData(
            ownerExchangeKey = exchangeKeyPair.publicKey,
            code = personalCode,
            memberVerificationSecret = memberVerificationSecret
        )

        val userKeys = SimpleKeyPairData(
            privateKey = exchangeKeyPair.privateKey,
            publicKey = exchangeKeyPair.publicKey
        )

        val payload = PersonalCodeFunctionData(
            keyId = derivedCode.loginPassword,
            encryptedCodeUserData = cryptoCore.encryptDataWithSimplePassword(
                data = BaseRemoteModule.formatter.encodeToString(codeUserData),
                password = derivedCode.backupPassword,
                userId = user.id
            ),
            encryptedCodeKeyData = cryptoCore.encryptDataWithSimplePassword(
                data = BaseRemoteModule.formatter.encodeToString(codeKeyData),
                password = derivedCode.backupPassword,
                userId = user.id
            ),
            encryptedCode = cryptoCore.encryptDataWithKey(
                data = personalCode,
                key = userKeyRepo.personalKey
            ),
            encryptedUserKeys = cryptoCore.encryptModel(
                data = userKeys,
                key = userKeyRepo.personalKey
            ),
            integrity = integrity
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
        // TODO key could be in hex format - use more characters
        // create crypto function to return random key encoded with wider character set
        val key = cryptoCore.getNewSymmetricKey()
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
                personalCodeMapper.remote2CoreRead(
                    code = code,
                    encryptedUserData = response.encryptedCodeUserData,
                    encryptedKeyData = response.encryptedCodeKeyData,
                    userId = response.userId
                ).takeIf { it.userData.ownerUserId == response.userId }
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