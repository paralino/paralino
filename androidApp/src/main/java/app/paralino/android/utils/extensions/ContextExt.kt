package app.paralino.android.utils.extensions

import android.content.*
import android.content.Context.BATTERY_SERVICE
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.os.BatteryManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri


val Context.batterLevel: Int
    get() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
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

fun Context.openUri(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW, uri)
    startActivity(Intent.createChooser(intent, "Open"))
}

fun Context.openUri(uri: String) {
    openUri(uri.toUri())
}

fun Context.hideKeyboardFrom(view: View) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}