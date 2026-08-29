package app.paralino.core.ui.components.dialogs

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import app.paralino.core.models.base.DeviceType
import app.paralino.core.provider.current
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Workaround for compose multiplatform 1.11.x change: https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.11.0
 * On iOS Dialogs and popups are now rendered differently, which cause the app to crash with IllegalArgumentException: updateRootConstraints called while measuring
 * It crashes if keyboard is visible and dialog is about to appear.
 * This now hides the keyboard and/or waits for it to be hidden before showing the dialog
 */
@Composable
fun IosKeyboardSafeDialog(content: @Composable () -> Unit) {
    if (DeviceType.current != DeviceType.iOS) {
        content()
        return
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeBottom = ime.getBottom(density)
    val keyboardWasVisible = imeBottom > 8 || isSoftwareKeyboardVisible()

    var showDialog by remember { mutableStateOf(!keyboardWasVisible) }

    LaunchedEffect(Unit) {
        if (showDialog) return@LaunchedEffect
        val startingImeBottom = ime.getBottom(density)
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (startingImeBottom > 8) {
            withTimeoutOrNull(500.milliseconds) {
                snapshotFlow { ime.getBottom(density) }.first { it <= 8 }
            }
        } else {
            delay(350.milliseconds)
        }
        withFrameNanos { }
        showDialog = true
    }

    if (showDialog) {
        content()
    }
}

internal expect fun isSoftwareKeyboardVisible(): Boolean
