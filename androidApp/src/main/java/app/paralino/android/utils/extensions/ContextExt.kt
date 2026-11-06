package app.paralino.android.utils.extensions

import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import app.paralino.core.resources.moko.*
import app.paralino.core.models.base.BackendResourceIDs


val Context.batterLevel: Int
    get() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

val Context.isBatteryCharging: Boolean
    get() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            this.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
    }

val Context.isLowPowerMode: Boolean
    get() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }

fun Context.isDarkThemeOn(): Boolean {
    return resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
}

fun Context.shareAndCopy(data: String, copyKey: String = app.paralino.android.BuildConfig.APPLICATION_ID) {
    copy(data, copyKey)
    share(data)
}

fun Context.share(data: String) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, data);
    startActivity(Intent.createChooser(intent, "Share"))
}

fun Context.copy(data: String, copyKey: String = app.paralino.android.BuildConfig.APPLICATION_ID + "_copyKey") {
    ContextCompat.getSystemService(this, ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(copyKey, data))
}

fun Context.contactSupport() {
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.setData(Uri.parse("mailto:")) // only email apps should handle this
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(BackendResourceIDs.supportEmail))
    intent.putExtra(Intent.EXTRA_SUBJECT, "Paralino support")

    val chooserIntent = Intent.createChooser(intent, MR.strings.setting_title_contactSupport.getString(this))
    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(chooserIntent)
}

fun Context.openNotificationChannelSettings(channel: String) {
    try {
        val notificationManager = this.getSystemService(Service.NOTIFICATION_SERVICE) as? NotificationManager

        if (notificationManager?.getNotificationChannel(channel) == null) {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, this@openNotificationChannelSettings.packageName)
            }
            this@openNotificationChannelSettings.startActivity(intent)
            return
        }

        val intent = Intent().apply {
            action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, this@openNotificationChannelSettings.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channel)
        }
        this@openNotificationChannelSettings.startActivity(intent)
    } catch (e: Exception) {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", this@openNotificationChannelSettings.packageName, null)
        }
        this@openNotificationChannelSettings.startActivity(intent)
    }
}