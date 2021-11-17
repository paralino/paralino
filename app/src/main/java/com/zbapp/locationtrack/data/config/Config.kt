package com.zbapp.locationtrack.data.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.zbapp.core.utils.IConfig
import com.zbapp.locationtrack.BuildConfig

class Config(context: Context): IConfig {

    companion object {
        const val keyToken = "prefs.profile.token"
        const val keyEmail = "prefs.profile.email"
        const val keyUserId = "prefs.profile.user_id"
        const val keyEmailConfirmed = "prefs.profile.email.confirmed"
        const val keyServerUrlMain = "prefs.server.url"
        const val keyServerKeyMain = "prefs.server.key"
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

    override var emailConfirmed: Boolean
        get() = secureSharedData.getBoolean(keyEmailConfirmed, false)
        set(value) { secureSharedData.edit { putBoolean(keyEmailConfirmed, value) } }

    override var mainServerUrl: String
        get() = secureSharedData.getString(keyServerUrlMain, null) ?: BuildConfig.serverUrl
        set(value) {
            if(value.isEmpty())
                secureSharedData.edit { remove(keyServerUrlMain) }
            else
                secureSharedData.edit { putString(keyServerUrlMain, value) }
        }

    override var mainServerKey: String
        get() = secureSharedData.getString(keyServerKeyMain, null) ?: BuildConfig.serverKey
        set(value) {
            if(value.isEmpty())
                secureSharedData.edit { remove(keyServerKeyMain) }
            else
                secureSharedData.edit { putString(keyServerKeyMain, value) }
        }

    override val collectionGroupsID: String get() = BuildConfig.keyCollectionGroups
    override val collectionLocationsID: String get() = BuildConfig.keyCollectionLocations
    override val collectionInvitesID: String get() = BuildConfig.keyCollectionInvites
    override val functionInviteCreateID: String get() = BuildConfig.keyFunctionInviteCreate
    override val functionInviteGetCodeID: String get() = BuildConfig.keyFunctionInviteGetCode
    override val functionInviteGetUrlID: String get() = BuildConfig.keyFunctionInviteGetUrl

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