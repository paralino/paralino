package app.paralino.core.domain.crypto.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.testutils.FakeCryptoServiceProvider
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CryptoAccountOperationV2Test {

    @Test
    fun verifyUserKeysChange_oldOnly_passesAndFailsWithHelpfulMessage() = runBlocking {
        val cryptoCore = CryptoCore(FakeCryptoServiceProvider())
        val op = CryptoAccountOperationV2(cryptoCore)

        val userId = "user_1"
        val password = "pw_old"
        val loginAlg = cryptoCore.getDefaultLoginKDFAlg()
        val signatureKeyPair = cryptoCore.getNewSignatureKeyPair()

        val generated = op.changeUserMasterKey(
            userId = userId,
            password = password,
            signatureKeyPair = signatureKeyPair,
            loginAlg = loginAlg
        )

        // In this function, oldMasterKey is expected to be the decrypted personal key.
        op.verifyUserKeysChange(
            oldKey = generated.encryptedKeyData,
            oldPassword = password,
            oldMasterKey = generated.userPersonalKey
        )

        val ex = assertFailsWith<IllegalStateException> {
            op.verifyUserKeysChange(
                oldKey = generated.encryptedKeyData,
                oldPassword = password,
                oldMasterKey = "wrong"
            )
        }
        assertEquals("Old key does not match", ex.message)
    }

    @Test
    fun verifyUserKeysChange_oldAndNew_passesAndDetectsMismatchCases() = runBlocking {
        val cryptoCore = CryptoCore(FakeCryptoServiceProvider())
        val op = CryptoAccountOperationV2(cryptoCore)

        val userId = "user_1"
        val oldPassword = "pw_old"
        val newPassword = "pw_new"
        val loginAlg = cryptoCore.getDefaultLoginKDFAlg()
        val signatureKeyPair = cryptoCore.getNewSignatureKeyPair()

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

        // success
        op.verifyUserKeysChange(
            oldKey = oldGenerated.encryptedKeyData,
            newKey = newGenerated.encryptedKeyData,
            newPassword = newPassword,
            oldMasterKey = oldGenerated.userPersonalKey,
            newMasterKey = newGenerated.userPersonalKey
        )

        // New key mismatch.
        assertEquals(
            "New key does not match",
            assertFailsWith<IllegalStateException> {
                op.verifyUserKeysChange(
                    oldKey = oldGenerated.encryptedKeyData,
                    newKey = newGenerated.encryptedKeyData,
                    newPassword = newPassword,
                    oldMasterKey = oldGenerated.userPersonalKey,
                    newMasterKey = "wrong"
                )
            }.message
        )

        // Private key mismatch.
        val privateMismatch = newGenerated.encryptedKeyData.copy(
            encryptedPrivateKey = cryptoCore.encryptDataWithKey(
                data = "different_private",
                key = newGenerated.userPersonalKey
            )
        )
        assertEquals(
            "Private key does not match",
            assertFailsWith<IllegalStateException> {
                op.verifyUserKeysChange(
                    oldKey = oldGenerated.encryptedKeyData,
                    newKey = privateMismatch,
                    newPassword = newPassword,
                    oldMasterKey = oldGenerated.userPersonalKey,
                    newMasterKey = newGenerated.userPersonalKey
                )
            }.message
        )

        // Public key mismatch (encrypted payload).
        val publicMismatch = newGenerated.encryptedKeyData.copy(
            encryptedPublicKey = cryptoCore.encryptDataWithKey(
                data = "different_public",
                key = newGenerated.userPersonalKey
            )
        )
        assertEquals(
            "Public key does not match",
            assertFailsWith<IllegalStateException> {
                op.verifyUserKeysChange(
                    oldKey = oldGenerated.encryptedKeyData,
                    newKey = publicMismatch,
                    newPassword = newPassword,
                    oldMasterKey = oldGenerated.userPersonalKey,
                    newMasterKey = newGenerated.userPersonalKey
                )
            }.message
        )

        // Public key mismatch (raw publicKey field).
        val publicKeyFieldMismatch = newGenerated.encryptedKeyData.copy(
            publicKey = "different"
        )
        assertEquals(
            "Public key does not match",
            assertFailsWith<IllegalStateException> {
                op.verifyUserKeysChange(
                    oldKey = oldGenerated.encryptedKeyData,
                    newKey = publicKeyFieldMismatch,
                    newPassword = newPassword,
                    oldMasterKey = oldGenerated.userPersonalKey,
                    newMasterKey = newGenerated.userPersonalKey
                )
            }.message
        )
    }
}

