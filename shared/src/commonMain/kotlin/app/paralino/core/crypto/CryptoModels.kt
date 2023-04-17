package app.paralino.core.crypto


class KeyPair(
    val privateKey: String,
    val publicKey: String
)

data class DerivedPasswords(
    val loginPassword: String,
    val backupPassword: String
)

data class DecryptedGroupKeyData(
    val version: Long,
    val data: String
)

class Data(val content: ByteArray) {
//    fun asString(hashProvider: ICryptoHashProvider): String {
//        return hashProvider.byteArrayToString(content)
//    }
//
//    fun asBase64String(hashProvider: ICryptoHashProvider): String {
//        return hashProvider.toBase64(content)
//    }
}

fun ByteArray.toData() = Data(this)

//fun String.base64ToData(hashProvider: ICryptoHashProvider): Data {
//    return hashProvider.fromBase64(this)
//}
//
//fun String.toData(hashProvider: ICryptoHashProvider): Data {
//    return hashProvider.toByteArray(this)
//}