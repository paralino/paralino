package app.paralino.android.crpyto

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.paralino.core.crypto.EncodingType
import app.paralino.core.crypto.KDFAlg
import app.paralino.core.crypto.KDFType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.domain.crypto.user.CryptoAccountOperationV2
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CryptoFrameworkLibsodiumInstrumentedTest {

    private val crypto = CryptoFrameworkLibsodium()

    @Test
    fun symmetricEncryptDecrypt_roundTrips() {
        val key = crypto.createNewSymmetricKey()
        val plain = "hello"

        val cipher = crypto.encryptDataWithSymmetricKey(plain, key)
        val decoded = crypto.decryptDataWithSymmetricKey(cipher, key)

        assertEquals(plain, decoded)
    }

    @Test
    fun symmetricEncryption_prefix_isAbsentForDefaultKey_andPresentForDerivedKey() {
        val plain = "hello"

        val defaultKey = crypto.createNewSymmetricKey()
        val defaultCipher = crypto.encryptDataWithSymmetricKey(plain, defaultKey)
        assertTrue(
            "Expected DEFAULT cipher to omit tags when skipTagsIfDefault=true, got: $defaultCipher",
            !defaultCipher.startsWith("@:")
        )

        val derivedKey = crypto.createNewSymmetricKeyForDerivation()
        val derivedCipher = crypto.encryptDataWithSymmetricKey(plain, derivedKey)
        assertTrue(
            "Expected DEFAULT_KEY_DERIVE cipher to include tags (payload present), got: $derivedCipher",
            derivedCipher.startsWith("@:")
        )
        assertTrue(
            "Expected tagged cipher to contain the :@ delimiter, got: $derivedCipher",
            derivedCipher.contains(":@")
        )

        val decoded = crypto.decryptDataWithSymmetricKey(derivedCipher, derivedKey)
        assertEquals(plain, decoded)
    }

    @Test
    fun keypairEncryptDecrypt_roundTrips_betweenTwoParties() {
        val alice = crypto.createNewCryptoKeyPair()
        val bob = crypto.createNewCryptoKeyPair()

        val plain = "secret message"
        val cipher = crypto.encryptDataWithKeyPair(
            data = plain,
            myPrivateKey = alice.privateKey,
            publicKey = bob.publicKey
        )
        val decoded = crypto.decryptDataWithKeyPair(
            data = cipher,
            myPrivateKey = bob.privateKey,
            publicKey = alice.publicKey
        )

        assertEquals(plain, decoded)
    }

    @Test
    fun signAndVerify_roundTrips() {
        val kp = crypto.createNewSignatureKeyPair()
        val plain = "signed payload"

        val signed = crypto.signData(plain, kp.privateKey)
        val verified = crypto.verifySignature(signed, kp.publicKey)

        assertNotNull(verified)
        assertEquals(plain, verified)
    }

    @Test
    fun deriveKeyFromPassword_isDeterministicForSameInputs() {
        val alg = KDFAlg(
            encoding = EncodingType.BASE64_URL.key,
            kdf = KDFType.ARGON2ID.key,
            memoryMb = 32,
            rounds = 2,
            keyBytes = 32
        )

        val p1 = runBlocking { crypto.deriveKeyFromPassword(password = "pw", salt = "salt", alg = alg) }
        val p2 = runBlocking { crypto.deriveKeyFromPassword(password = "pw", salt = "salt", alg = alg) }
        val p3 = runBlocking { crypto.deriveKeyFromPassword(password = "pw", salt = "salt2", alg = alg) }

        assertEquals(p1, p2)
        assertTrue(p1 != p3)
    }

    @Test
    fun verifyUserKeysChange_oldOnly_passesAndFails() = runBlocking {
        val core = CryptoCore(crypto)
        val op = CryptoAccountOperationV2(core)

        val userId = "user_1"
        val password = "pw_old"
        val loginAlg = core.getDefaultLoginKDFAlg()
        val signatureKeyPair = core.getNewSignatureKeyPair()

        val generated = op.changeUserMasterKey(
            userId = userId,
            password = password,
            signatureKeyPair = signatureKeyPair,
            loginAlg = loginAlg
        )

        // should pass
        op.verifyUserKeysChange(
            oldKey = generated.encryptedKeyData,
            oldPassword = password,
            oldMasterKey = generated.userPersonalKey
        )

        // should fail for wrong expected key
        try {
            op.verifyUserKeysChange(
                oldKey = generated.encryptedKeyData,
                oldPassword = password,
                oldMasterKey = generated.userPersonalKey + "_wrong"
            )
            throw AssertionError("Expected verifyUserKeysChange to fail")
        } catch (e: IllegalStateException) {
            assertEquals("Old key does not match", e.message)
        }
    }

    @Test
    fun verifyUserKeysChange_oldAndNew_passesAndDetectsNewMasterKeyMismatch() = runBlocking {
        val core = CryptoCore(crypto)
        val op = CryptoAccountOperationV2(core)

        val userId = "user_1"
        val oldPassword = "pw_old"
        val newPassword = "pw_new"
        val loginAlg = core.getDefaultLoginKDFAlg()

        // keep identity keys same between old and new,
        // otherwise verifyUserKeysChange is expected to fail (private/public mismatch).
        val signatureKeyPair = core.getNewSignatureKeyPair()

        val oldGenerated = op.changeUserMasterKey(
            userId = userId,
            password = oldPassword,
            signatureKeyPair = signatureKeyPair,
            loginAlg = loginAlg
        )
        val newGenerated = op.changeUserMasterKey(
            userId = userId,
            password = newPassword,
            signatureKeyPair = signatureKeyPair,
            loginAlg = loginAlg
        )

        assertNotEquals(oldGenerated.userPersonalKey, newGenerated.userPersonalKey)

        // should pass
        op.verifyUserKeysChange(
            oldKey = oldGenerated.encryptedKeyData,
            newKey = newGenerated.encryptedKeyData,
            newPassword = newPassword,
            oldMasterKey = oldGenerated.userPersonalKey,
            newMasterKey = newGenerated.userPersonalKey
        )

        // should fail on wrong new master key
        try {
            op.verifyUserKeysChange(
                oldKey = oldGenerated.encryptedKeyData,
                newKey = newGenerated.encryptedKeyData,
                newPassword = newPassword,
                oldMasterKey = oldGenerated.userPersonalKey,
                newMasterKey = newGenerated.userPersonalKey + "_wrong"
            )
            throw AssertionError("Expected verifyUserKeysChange(new) to fail")
        } catch (e: IllegalStateException) {
            assertEquals("New key does not match", e.message)
        }
    }
}

