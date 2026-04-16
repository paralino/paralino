package app.paralino.core.utils.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight

@Composable
fun String.annotatedBoldCompose(
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.Unspecified
): AnnotatedString {
    return remember(this, fontWeight, textColor) {
        this.annotatedBold(fontWeight = fontWeight, textColor = textColor)
    }
}