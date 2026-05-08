package app.paralino.core.utils.extensions

import android.content.Context
import app.paralino.core.presentation.map.MapHomeScreenEvent

fun Context.openNavigation(event: MapHomeScreenEvent.OpenExternalNavigation) {
    openUri(
        uri = "geo:${event.lat},${event.lng}?q=${event.lat},${event.lng}${event.name?.let { "(${it})" } ?: ""}"
    )
}