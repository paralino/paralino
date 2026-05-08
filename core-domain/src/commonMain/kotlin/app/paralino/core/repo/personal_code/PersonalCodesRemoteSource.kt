package app.paralino.core.repo.personal_code

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.mappers.PersonalCodeMapperCore
import app.paralino.core.models.raw.PersonalCodeCreateData
import app.paralino.core.models.raw.PersonalCodePreKeyData
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.raw.PersonalCodeReadPayload
import app.paralino.core.models.raw.PersonalCodeResponse
import app.paralino.core.models.raw.PersonalCodeUpdateData
import app.paralino.core.models.raw.PersonalPreKeyRaw
import app.paralino.core.models.raw.PersonalPreKeyUpdateData
import app.paralino.core.models.user.PersonalCodeKeyData
import app.paralino.core.models.user.PersonalCodeUserData
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserMeData
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.extensions.nowUTC

class PersonalCodesRemoteSource constructor(
    private val functionsApi: IFunctionsApi,
    private val docApi: IDocumentsApi,
    private val appConfig: AppConfigRepository,
    private val personalCodeMapper: PersonalCodeMapperCore,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeysLocalSource
) {

    suspend fun updateCodeUserData(personalCode: String, userId: String, userMeData: UserMeData) {
        val derivedCode = cryptoCore.deriveParalinoIdMasterKeys(
            code = personalCode,
            userId = userId
        )
        val myKey = userKeyRepo.signatureKeyPair()

        val codeUserData = PersonalCodeUserData(
            ownerUserId = userId,
            ownerName = userMeData.userName,
            ownerImage = userMeData.userImage,
            ownerIdentityKey = myKey.publicKey,
        )

        val data = PersonalCodeUpdateData(
            keyId = derivedCode.loginPassword,
            encryptedCodeUserData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(codeUserData),
                key = derivedCode.backupPassword
            ),
            keys = createPreKeys(
                personalCode = personalCode,
                encryptionKey = derivedCode.backupPassword
            )
        )

        functionsApi.runAndWait(
            function = ServerFunction.PERSONAL_CODE_UPDATE,
            data = data
        )
            .let { response ->
                if (!response.isSuccess) {
                    throw BaseRemoteException.fromCode(
                        code = response.statusCode,
                        message = "Unable to update personal code",
                        cause = Exception(response.stderr)
                    )
                }
            }
    }

    suspend fun tryCreatingCode(user: UserMe, userMeData: UserMeData?) {

        var personalCode: String? = null
        do {
            val newCode = cryptoCore.randomId(length = 8, alphabet = AlphabetType.SAFE)
            val charGroups = newCode.groupBy { it }

            // only allow codes with enough different characters (i.e. 1111-2222 is not allowed)
            // one character can only appear max 3 times, and there has to be at least 4 different character
            if (charGroups.size >= 4 && charGroups.maxOf { it.value.size } <= 3) {
                personalCode = newCode
            }
        } while (personalCode == null)

        val derivedCode = cryptoCore.deriveParalinoIdMasterKeys(
            code = personalCode,
            userId = user.id
        )
        val myKey = userKeyRepo.signatureKeyPair()

        val codeUserData = PersonalCodeUserData(
            ownerUserId = user.id,
            ownerName = userMeData?.userName ?: user.data?.userName ?: "",
            ownerImage = userMeData?.userImage ?: user.data?.userImage,
            ownerIdentityKey = myKey.publicKey,
        )

        val keys = createPreKeys(
            personalCode = personalCode,
            encryptionKey = derivedCode.backupPassword
        )

        val userKey = userKeyRepo.activePersonalKeyWithVersion()
        val data = PersonalCodeCreateData(
            keyId = derivedCode.loginPassword,
            encryptedCodeUserData = cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(codeUserData),
                key = derivedCode.backupPassword
            ),
            encryptedCode = cryptoCore.encryptDataWithKey(
                data = personalCode,
                key = userKey.key
            ),
            userKeyVersion = userKey.version,
            keys = keys
        )

        functionsApi.runAndWait(
            function = ServerFunction.PERSONAL_CODE_CREATE,
            data = data
        )
            .let { response ->
                if (!response.isSuccess) {
                    throw BaseRemoteException.fromCode(
                        code = response.statusCode,
                        message = "Unable to create personal code",
                        cause = Exception(response.stderr)
                    )
                }
            }
    }

    suspend fun restockPreKeysIfNeeded(
        userId: String,
        personalCode: String
    ) {
        val currentUnusedKeys = docApi.listAllDocuments(
            databaseType = BackendDatabaseType.KEYS,
            collectionId = appConfig.resourceIds.collectionPersonalIdKeysID,
        ).map { it.parsePreKey() }
            .filter { !it.encryptedCodeKeyData.isNullOrBlank() }

        if (currentUnusedKeys.size > 3) {
            logg.v("Found ${currentUnusedKeys.size} unused pre keys, skip restock")
            return
        }
        logg.v("Found ${currentUnusedKeys.size} unused pre keys, try to restock")

        val derivedCode = cryptoCore.deriveParalinoIdMasterKeys(
            code = personalCode,
            userId = userId
        )

        val keys = createPreKeys(
            personalCode = personalCode,
            encryptionKey = derivedCode.backupPassword
        )

        val data = PersonalPreKeyUpdateData(
            personalCodeKeyId = derivedCode.loginPassword,
            keys = keys
        )

        functionsApi.runAndWait(
            function = ServerFunction.KEYS_UPDATE_CODE,
            data = data
        )
            .let { response ->
                if (!response.isSuccess) {
                    throw BaseRemoteException.fromCode(
                        code = response.statusCode,
                        message = "Unable to upload pre keys",
                        cause = Exception(response.stderr)
                    )
                }
            }
    }

    private suspend fun createPreKeys(
        personalCode: String,
        encryptionKey: String,
        count: Int = 6
    ): List<PersonalCodePreKeyData> {
        val signatureKeys = userKeyRepo.signatureKeyPair()
        val userKey = userKeyRepo.activePersonalKeyWithVersion()

        val integrity = GroupIntegrity(
            timeUtc = nowUTC().toString(),
            // this signature will always be invalid, but we are passing it for consistency
            // this also ensures user will always rotate keys when added to new group
            signature = ""
        ).let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = userKey.key,
            )
        }

        return (0 until count).map {

            val exchangeKeyPair = cryptoCore.getNewCryptoKeyPair()

            val keyId = cryptoCore.hashProvider.computeHash(
                data = exchangeKeyPair.publicKey,
                type = HashType.GENERIC_256
            )

            val codeKeyData = PersonalCodeKeyData(
                signedOwnerKxPublicKey = cryptoCore.signData(
                    data = exchangeKeyPair.publicKey,
                    myPrivateKey = signatureKeys.privateKey
                ),
                code = personalCode,
                keyId = keyId
            )

            val userKeys = SimpleKeyPairData(
                privateKey = exchangeKeyPair.privateKey,
                publicKey = exchangeKeyPair.publicKey
            )

            PersonalCodePreKeyData(
                encryptedUserKeys = cryptoCore.encryptModel(
                    data = userKeys,
                    key = userKey.key
                ),
                encryptedCodeKeyData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.formatter.encodeToString(codeKeyData),
                    key = encryptionKey
                ),
                integrity = integrity,
                userKeyVersion = userKey.version,
                keyId = keyId
            )
        }
    }

    suspend fun getMyPersonalCodes(): List<PersonalCodeRaw> {
        return docApi.listAllDocuments(
            databaseType = BackendDatabaseType.DATA,
            collectionId = appConfig.resourceIds.collectionPersonalCodeID
        )
            .map { PersonalCodeRaw.from(it) }
    }

    suspend fun getUserInfoFromCode(code: String): UserPersonalCodeData {
        val keyId = cryptoCore.deriveParalinoIdAuthKey(code = code)
        val response = functionsApi.runAndWait(
            function = ServerFunction.PERSONAL_CODE_READ_V2,
            data = PersonalCodeReadPayload(keyId = keyId)
        )

        if (!response.isSuccess) {
            throw BaseRemoteException.fromCode(
                code = response.statusCode,
                message = "Error reading user code",
                cause = Exception(response.stderr)
            )
        } else {
            val responseContent = response.response<PersonalCodeResponse>().data
            return personalCodeMapper.remote2CoreRead(
                code = code,
                encryptedUserData = responseContent.encryptedCodeUserData,
                encryptedKeyData = responseContent.encryptedCodeKeyData,
                userId = responseContent.userId
            )
        }
    }

    suspend fun revokeAllCodes() {
        functionsApi.runAndWait(
            function = ServerFunction.PERSONAL_CODE_DELETE,
            data = null
        )
            .let { response ->
                if (!response.isSuccess) {
                    throw BaseRemoteException.fromCode(
                        code = response.statusCode,
                        message = "Unable to revoke personal code",
                        cause = Exception(response.stderr)
                    )
                }
            }
    }

    private fun Map<String, Any>.parsePreKey(): PersonalPreKeyRaw {
        return PersonalPreKeyRaw.from(this)
    }
}
