package app.paralino.core.repo.personal_code

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.mappers.PersonalCodeMapperCore
import app.paralino.core.models.raw.GenericFunctionPayload
import app.paralino.core.models.raw.PersonalCodeCreateData
import app.paralino.core.models.raw.PersonalCodePreKeyData
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.raw.PersonalCodeResponse
import app.paralino.core.models.raw.PersonalCodeUpdateData
import app.paralino.core.models.raw.PersonalPreKeyRaw
import app.paralino.core.models.raw.PersonalPreKeyUpdateData
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

    suspend fun updateCodeUserData(personalCode: String, userId: String, userMeData: UserMeData) {
        val derivedCode = cryptoCore.derivePassword(personalCode)
        val myKey = userKeyRepo.latestKeyPair()

        val codeUserData = PersonalCodeUserData(
            ownerUserId = userId,
            ownerName = userMeData.userName,
            ownerImage = userMeData.userImage,
            ownerIdentityKey = myKey.publicKey,
        )

        val integrity = GroupIntegrity(
            timeUtc = nowUTC().toString(),
            // this signature will always be invalid, but we are passing it for consistency
            // this also ensures user will always rotate keys when added to new group
            signature = ""
        ).let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = userKeyRepo.personalKey(),
            )
        }

        val data = PersonalCodeUpdateData(
            keyId = derivedCode.loginPassword,
            encryptedCodeUserData = cryptoCore.encryptDataWithSimplePassword(
                data = BaseRemoteModule.formatter.encodeToString(codeUserData),
                password = derivedCode.backupPassword,
                userId = userId
            ),
            keys = createPreKeys(userId, personalCode, integrity)
        )

        val payload = GenericFunctionPayload(
            route = "update",
            payload = data
        )

        functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionPersonalCodeCreateID,
            data = BaseRemoteModule.formatter.encodeToString(payload)
        )
            .let {
                if (!it.isSuccess) {
                    throw BaseRemoteException.fromCode(
                        null,
                        "Unable to update personal code",
                        Exception(it.stderr)
                    )
                }
            }
    }

    suspend fun tryCreatingCode(user: UserMe, userMeData: UserMeData?) {

        val personalCode = createPersonalCodeString()
        val derivedCode = cryptoCore.derivePassword(personalCode)
        val myKey = userKeyRepo.latestKeyPair()

        val codeUserData = PersonalCodeUserData(
            ownerUserId = user.id,
            ownerName = userMeData?.userName ?: user.data?.userName ?: "",
            ownerImage =  userMeData?.userImage ?: user.data?.userImage,
            ownerIdentityKey = myKey.publicKey,
        )

        val integrity = GroupIntegrity(
            timeUtc = nowUTC().toString(),
            // this signature will always be invalid, but we are passing it for consistency
            // this also ensures user will always rotate keys when added to new group
            signature = ""
        ).let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = userKeyRepo.personalKey(),
            )
        }

        val keys = createPreKeys(user.id, personalCode, integrity)

        val data = PersonalCodeCreateData(
            keyId = derivedCode.loginPassword,
            encryptedCodeUserData = cryptoCore.encryptDataWithSimplePassword(
                data = BaseRemoteModule.formatter.encodeToString(codeUserData),
                password = derivedCode.backupPassword,
                userId = user.id
            ),
            encryptedCode = cryptoCore.encryptDataWithKey(
                data = personalCode,
                key = userKeyRepo.personalKey()
            ),
            keys = keys
        )

        val payload = GenericFunctionPayload(
            route = "create",
            payload = data
        )

        functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionPersonalCodeCreateID,
            data = BaseRemoteModule.formatter.encodeToString(payload)
        )
            .let {
                if (!it.isSuccess) {
                    throw BaseRemoteException.fromCode(
                        null,
                        "Unable to create personal code",
                        Exception(it.stderr)
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
            collectionId = remoteConfig.resourceIds.collectionPersonalIdKeysID,
        ).map { it.parsePreKey() }
            .filter { !it.encryptedCodeKeyData.isNullOrBlank() }

        if(currentUnusedKeys.size > 3) {
            logg.v("Found ${currentUnusedKeys.size} unused pre keys, skip restock")
            return
        }
        logg.v("Found ${currentUnusedKeys.size} unused pre keys, try to restock")

        val derivedCode = cryptoCore.derivePassword(personalCode)
        val integrity = GroupIntegrity(
            timeUtc = nowUTC().toString(),
            // this signature will always be invalid, but we are passing it for consistency
            // this also ensures user will always rotate keys when added to new group
            signature = ""
        ).let {
            cryptoCore.encryptDataWithKey(
                data = BaseRemoteModule.formatter.encodeToString(it),
                key = userKeyRepo.personalKey(),
            )
        }
        val keys = createPreKeys(userId, personalCode, integrity)

        val data = PersonalPreKeyUpdateData(
            personalCodeKeyId = derivedCode.loginPassword,
            keys = keys
        )

        val payload = GenericFunctionPayload(
            route = "restock-keys",
            payload = data
        )

        functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionPersonalCodeCreateID,
            data = BaseRemoteModule.formatter.encodeToString(payload)
        )
            .let {
                if (!it.isSuccess) {
                    throw BaseRemoteException.fromCode(
                        null,
                        "Unable to upload pre keys",
                        Exception(it.stderr)
                    )
                }
            }
    }

    private suspend fun createPreKeys(
        userId: String,
        personalCode: String,
        integrity: String,
        count: Int = 4
    ): List<PersonalCodePreKeyData> {
        val myKey = userKeyRepo.latestKeyPair()
        val derivedCode = cryptoCore.derivePassword(personalCode)

        return (0 until count).map {

            val exchangeKeyPair = cryptoCore.getNewCryptoKeyPair()

            val keyId = cryptoCore.hashProvider.computeHash(
                data = exchangeKeyPair.publicKey,
                type = HashType.SHA256
            )

            val codeKeyData = PersonalCodeKeyData(
                signedOwnerKxPublicKey = cryptoCore.signData(
                    data = exchangeKeyPair.publicKey,
                    myPrivateKey = myKey.privateKey
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
                    key = userKeyRepo.personalKey()
                ),
                encryptedCodeKeyData = cryptoCore.encryptDataWithSimplePassword(
                    data = BaseRemoteModule.formatter.encodeToString(codeKeyData),
                    password = derivedCode.backupPassword,
                    userId = userId
                ),
                integrity = integrity,
                keyId = keyId
            )
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
                )
            }!!
    }

    suspend fun revokeAllCodes() {
        val payload = GenericFunctionPayload(
            route = "revoke",
            payload = ""
        )
        functionsApi.runAndWait(
            functionId = remoteConfig.resourceIds.functionPersonalCodeCreateID,
            data = BaseRemoteModule.formatter.encodeToString(payload)
        )
            .let {
                if (!it.isSuccess) {
                    throw BaseRemoteException.fromCode(
                        null,
                        "Unable to revoke personal code",
                        Exception(it.stderr)
                    )
                }
            }
    }

    private fun Map<String, Any>.parsePreKey(): PersonalPreKeyRaw {
        return PersonalPreKeyRaw.from(this)
    }
}
