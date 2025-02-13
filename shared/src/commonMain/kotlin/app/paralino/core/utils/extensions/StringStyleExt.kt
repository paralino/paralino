package app.paralino.core.utils.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

fun String.annotatedBold(
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.Unspecified
): AnnotatedString {
    val ranges = this.splitRanges("**")
    val cleanString = this.replace("**", "")
    return AnnotatedString(
        text = cleanString,
        spanStyles = ranges.map {
            AnnotatedString.Range(
                item = SpanStyle(fontWeight = fontWeight, color = textColor),
                start = it.second.first,
                end = it.second.last
            )
        }
    )
}

@Composable
fun String.annotatedBoldCompose(
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.Unspecified
): AnnotatedString {
    return remember(this, fontWeight, textColor) {
        this.annotatedBold(fontWeight = fontWeight, textColor = textColor)
    }
}