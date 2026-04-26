package app.paralino.core.domain.crypto.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.EncodingType
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.KDFType
import app.paralino.core.crypto.testutils.FakeCryptoServiceProvider
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CryptoDerivationTest {

    @Test
    fun deriveUserAuthKey_usesExpectedContextAndDerivedKeyId() = runBlocking {
        val core = CryptoCore(FakeCryptoServiceProvider())
        val op = CryptoAccountOperationV2(core)

        val email = "e@paralino.com"
        val password = "pw"
        val alg = core.getDefaultLoginKDFAlg()

        val derived = op.deriveUserAuthKey(email = email, password = password, alg = alg)

        val seed = "kdf(${password}|${email}|${alg.kdf}|${alg.rounds}|${alg.memoryMb}|${alg.keyBytes}|${alg.encoding})"
        val master = "kdf(${seed}|${password}|${alg.kdf}|${alg.rounds}|${alg.memoryMb}|${alg.keyBytes}|${alg.encoding})"
        val expectedLoginKey = "derive(${master}|0|prlnauth)"

        assertEquals(expectedLoginKey, derived.loginKey)
        assertEquals(alg, derived.kdfAlg)
    }

    @Test
    fun deriveParalinoIdAuthKey_hashesMasterKeyWithExpectedEncoding() = runBlocking {
        val core = CryptoCore(FakeCryptoServiceProvider())
        val code = "123456"

        val authKey = core.deriveParalinoIdAuthKey(code = code)

        // CryptoCore.deriveParalinoIdAuthKey uses FAST KDF parameters.
        val masterKey = "kdf(${code}|Fleur_de_sel|${KDFType.ARGON2ID.key}|2|32|32|${EncodingType.BASE64_URL.key})"
        val expected = "hash(${HashType.GENERIC_256.bytes}|${EncodingType.BASE64_URL.key})::${masterKey}"

        assertEquals(expected, authKey)
    }

    @Test
    fun deriveParalinoIdMasterKeys_returnsLoginAndBackupPasswords() = runBlocking {
        val core = CryptoCore(FakeCryptoServiceProvider())
        val code = "abc"
        val userId = "user"

        val derived = core.deriveParalinoIdMasterKeys(code = code, userId = userId)

        val authKey = core.deriveParalinoIdAuthKey(code = code)
        val masterKey = "kdf(${code}|${userId}|${KDFType.ARGON2ID.key}|2|32|32|${EncodingType.BASE64_URL.key})"

        assertEquals(authKey, derived.loginPassword)
        assertEquals(masterKey, derived.backupPassword)
    }
}

