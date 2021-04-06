package com.zbapp.locationtrack.data.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.zbapp.core.utils.IConfig

class Config(context: Context): IConfig {

    companion object {
        const val keyToken = "prefs.profile.token"
        const val keyEmail = "prefs.profile.email"
        const val keyUserId = "prefs.profile.user_id"
    }

    private val sharedData =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val secureSharedData by lazy {
            EncryptedSharedPreferences(context, "shared_prefs_user", MasterKey(context))
    }


    override var token: String
        get() = secureSharedData.getString(keyToken, "") ?: ""
        set(value) { secureSharedData.edit { putString(keyToken, value) } }

    override var email: String
        get() = secureSharedData.getString(keyEmail, "") ?: ""
        set(value) { secureSharedData.edit { putString(keyEmail, value) } }

    override var userId: String
        get() = secureSharedData.getString(keyUserId, "") ?: ""
        set(value) { secureSharedData.edit { putString(keyUserId, value) } }



    override fun clear() {
        sharedData.edit { clear() }
        secureSharedData.edit { clear() }
    }


    inline fun <reified T: Enum<T>> SharedPreferences.getEnum(key: String) : T?{
        try {
            return enumValueOf<T>(this.getString(key, null) ?: return null)
        } catch (e: Exception) {
            return null
        }
    }

}