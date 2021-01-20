package com.zbapp.locationtrack.data.config

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class Config(context: Context) {

    private val sharedData =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val secureSharedData by lazy {
            EncryptedSharedPreferences(context, "shared_prefs_user", MasterKey(context))
    }





    private fun SharedPreferences.edit(action: SharedPreferences.Editor.() -> Unit) {
        this.edit().also { action(it) }.apply()
    }

    inline fun <reified T: Enum<T>> SharedPreferences.getEnum(key: String) : T?{
        try {
            return enumValueOf<T>(this.getString(key, null) ?: return null)
        } catch (e: Exception) {
            return null
        }
    }

}