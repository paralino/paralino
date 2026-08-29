package app.paralino.core.ui.components.dialogs

import platform.UIKit.UIApplication
import platform.UIKit.UITextField
import platform.UIKit.UITextInputProtocol
import platform.UIKit.UITextView
import platform.UIKit.UIView

internal actual fun isSoftwareKeyboardVisible(): Boolean {
    val keyWindow = UIApplication.sharedApplication.keyWindow ?: return false
    if (keyWindow.hidden) return false
    return keyWindow.findTextInputFirstResponder() != null
}

private fun UIView.findTextInputFirstResponder(): UIView? {
    if (isFirstResponder() && isTextInput()) return this
    subviews.forEach { child ->
        (child as? UIView)?.findTextInputFirstResponder()?.let { return it }
    }
    return null
}

private fun UIView.isTextInput(): Boolean {
    return this is UITextField || this is UITextView || this is UITextInputProtocol
}
