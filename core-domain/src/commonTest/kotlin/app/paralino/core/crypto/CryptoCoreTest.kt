package app.paralino.core.crypto

import app.paralino.core.crypto.testutils.FakeCryptoServiceProvider
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CryptoCoreTest {

    @Test
    fun getDefaultLoginKDFAlg_isStable() {
        val core = CryptoCore(FakeCryptoServiceProvider())
        val alg = core.getDefaultLoginKDFAlg()

        assertEquals(KDFType.ARGON2ID.key, alg.kdf)
        assertEquals(64L, alg.memoryMb)
        assertEquals(3L, alg.rounds)
        assertEquals(32, alg.keyBytes)
        assertEquals(EncodingType.BASE64_URL.key, alg.encoding)
    }

    @Test
    fun getDefaultDataKDFAlg_isStable() {
        val core = CryptoCore(FakeCryptoServiceProvider())
        val alg = core.getDefaultDataKDFAlg()

        assertEquals(KDFType.ARGON2ID.key, alg.kdf)
        assertEquals(128L, alg.memoryMb)
        assertEquals(5L, alg.rounds)
        assertEquals(32, alg.keyBytes)
        assertEquals(EncodingType.BASE64_URL.key, alg.encoding)
    }

    @Test
    fun getPlaceEventKey_differsForEnterVsExit() {
        val core = CryptoCore(FakeCryptoServiceProvider())
        val seed = "seed"

        val enterKey = core.getPlaceEventKey(seed = seed, isEnter = true)
        val exitKey = core.getPlaceEventKey(seed = seed, isEnter = false)

        assertNotEquals(enterKey, exitKey)
        // Deterministic fake hash ensures we can validate the exact input.
        assertEquals(
            "hash(${HashType.GENERIC_256.bytes}|${EncodingType.BASE64_URL.key})::${seed}_enter",
            enterKey
        )
        assertEquals(
            "hash(${HashType.GENERIC_256.bytes}|${EncodingType.BASE64_URL.key})::${seed}_exit",
            exitKey
        )
    }

    @Test
    fun verifyKeys_passesForGeneratedKeys() {
        val core = CryptoCore(FakeCryptoServiceProvider())
        val masterKey = "master"
        val personalKey = "personal"
        val identityKeyPair = SimpleKeyPairData(privateKey = "id_priv", publicKey = "id_pub")

        val encryptedPersonalKey = runBlocking {
            core.encryptDataWithKey(data = personalKey, key = masterKey)
        }
        val encryptedPrivate = runBlocking {
            core.encryptDataWithKey(data = identityKeyPair.privateKey, key = personalKey)
        }
        val encryptedPublic = runBlocking {
            core.encryptDataWithKey(data = identityKeyPair.publicKey, key = personalKey)
        }

        val raw = app.paralino.core.models.raw.UserCryptoKeyRaw(
            id = "",
            ownerUserId = "u1",
            encryptedSymmetricKey = encryptedPersonalKey,
            encryptedPrivateKey = encryptedPrivate,
            encryptedPublicKey = encryptedPublic,
            publicKey = identityKeyPair.publicKey,
            oldMasterKeys = null,
            loginSalt = "ls",
            dataSalt = "ds",
            loginAlg = "",
            dataAlg = "",
            schemeVersion = null,
            userKeyVersion = 0
        )

        val keys = GeneratedUserKeys(
            userPersonalKey = personalKey,
            signatureKeyPair = identityKeyPair,
            encryptedKeyData = raw
        )

        runBlocking {
            core.verifyKeys(keys = keys, identityKeyPair = identityKeyPair, key = masterKey)
        }
    }
}

