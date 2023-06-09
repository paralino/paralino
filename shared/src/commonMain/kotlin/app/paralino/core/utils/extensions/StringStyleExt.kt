package app.paralino.core.utils.extensions

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