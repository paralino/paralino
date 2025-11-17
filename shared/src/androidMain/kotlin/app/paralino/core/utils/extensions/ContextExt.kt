package app.paralino.core.utils.extensions

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import app.paralino.core.presentation.map.MapHomeScreenEvent


fun Context.share(data: String) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, data);
    startActivity(Intent.createChooser(intent, "Share"))
}

fun Context.openUri(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW, uri)
    val chooserIntent = Intent.createChooser(intent, "Open")
    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(chooserIntent)
}

fun Context.openUri(uri: String) {
    openUri(uri.toUri())
}

fun Context.openNavigation(event: MapHomeScreenEvent.OpenExternalNavigation) {
    openUri(
        uri = "geo:${event.lat},${event.lng}?q=${event.lat},${event.lng}${event.name?.let { "(${it})" } ?: ""}"
    )
}

fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
