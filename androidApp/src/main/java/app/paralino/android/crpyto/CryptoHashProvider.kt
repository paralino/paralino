package app.paralino.android.crpyto

import android.util.Base64
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.ICryptoHashProvider
import java.security.MessageDigest

//class CryptoHashProvider: ICryptoHashProvider {
//
//    override fun computeHash(data: String, type: HashType): String {
//        val algo = when(type) {
//            HashType.SHA256 -> "SHA-256"
//            HashType.SHA512 -> "SHA-512"
//        }
//        return MessageDigest.getInstance(algo).digest(data)
//    }
//}