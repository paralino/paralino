package app.paralino.core.utils.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

fun String.splitRanges(wrappedChars: String): List<Pair<String, IntRange>> {
    val ranges = mutableListOf<Pair<String, IntRange>>()
    var isWrapped = false
    var start = 0
    this.split(wrappedChars).forEach {
        if(isWrapped) ranges.add(it to IntRange(start, start + it.length))
        isWrapped = !isWrapped
        start += it.length
    }
    return ranges.filter { pair ->
        val it = pair.second
        it.first >= 0 && it.last <= this.length && it.first < it.last
    }
}

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