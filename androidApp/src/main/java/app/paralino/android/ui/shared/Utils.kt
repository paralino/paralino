package app.paralino.android.ui.shared

import android.content.Context
import android.content.res.Resources
import android.text.SpannableString
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.utils.extensions.attributedBold
import app.paralino.core.utils.extensions.errorString

fun Context.errorString(error: Throwable?): SpannableString {
    return errorStringClean(error).attributedBold(this)
}

fun Context.errorStringClean(error: Throwable?): String {
    return error.errorString(this)
}

fun Throwable?.errorString(context: Context): String {
    return this.errorString(context.resources)
}

fun Throwable?.errorString(resources: Resources): String {
    return this.errorString()
}

@Composable
fun Throwable?.errorString(): String {
    return this.errorString(LocalContext.current)
}