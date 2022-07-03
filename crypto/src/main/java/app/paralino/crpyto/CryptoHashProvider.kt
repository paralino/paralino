package app.paralino.crpyto

import android.util.Base64
import app.paralino.core.crypto.Data
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.ICryptoHashProvider
import app.paralino.core.crypto.toData
import java.security.MessageDigest

class CryptoHashProvider: ICryptoHashProvider {

    override fun toBase64(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    override fun fromBase64(data: String): Data {
        return Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP).toData()
    }

    override fun toByteArray(data: String): Data {
        return data.encodeToByteArray(throwOnInvalidSequence = true).toData()
    }

    override fun byteArrayToString(data: ByteArray): String {
        return data.decodeToString(throwOnInvalidSequence = false)
    }

    override fun computeHash(data: ByteArray, type: HashType): Data {
        val algo = when(type) {
            HashType.SHA256 -> "SHA-256"
            HashType.SHA512 -> "SHA-512"
        }
        return MessageDigest.getInstance(algo).digest(data).toData()
    }
}