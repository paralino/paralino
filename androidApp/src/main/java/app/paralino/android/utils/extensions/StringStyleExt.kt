package app.paralino.android.utils.extensions

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.TypefaceSpan
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import app.paralino.core.utils.extensions.splitRanges
import app.paralino.android.R


class CustomTypefaceSpan(family: String, private val newType: Typeface) : TypefaceSpan(family) {

    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeFace(ds, newType)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, newType)
    }

    private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
        val oldStyle: Int
        val old = paint.typeface
        if (old == null) {
            oldStyle = 0
        } else {
            oldStyle = old.style
        }

        val fake = oldStyle and tf.style.inv()
        if (fake and Typeface.BOLD != 0) {
            paint.isFakeBoldText = true
        }

        if (fake and Typeface.ITALIC != 0) {
            paint.textSkewX = -0.25f
        }

        paint.typeface = tf
    }
}

fun Context.attributedBold(@StringRes res: Int, extraSpans: List<SpanContainer> = listOf()): SpannableString {
    return this.getString(res).attributedBold(this, extraSpans)
}

fun String.attributedBold(context: Context, extraSpans: List<SpanContainer> = listOf()): SpannableString {
    val ranges = this.splitRanges("**")
    val cleanString = this.replace("**", "")
    val typeface = ResourcesCompat.getFont(context, R.font.font_montserrat_semibold) ?: return SpannableString("")
    val spannableString = SpannableString(cleanString)
    ranges.forEach { range ->
        spannableString.setSpan(
            CustomTypefaceSpan("", typeface),
            range.second.first,
            range.second.last,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        extraSpans.forEach {
            if(it.key == range.first)
                spannableString.setSpan(
                    it.span,
                    range.second.first,
                    range.second.last,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
        }
    }
    return spannableString
}

fun linkSpan(context: Context, onClick: () -> Unit): ClickableSpan {
    return object : ClickableSpan() {
        override fun onClick(textView: View) {
            onClick.invoke()
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.color = ContextCompat.getColor(context, R.color.accentDark)
            ds.isUnderlineText = true
            ds.bgColor = Color.TRANSPARENT
        }
    }
}

data class SpanContainer(val key: String, val span: Any)

fun String.annotatedBold(): AnnotatedString {
    val ranges = this.splitRanges("**")
    val cleanString = this.replace("**", "")
    return AnnotatedString(cleanString, spanStyles = ranges.map {
        AnnotatedString.Range(
            item = SpanStyle(fontWeight = FontWeight.Bold),
            start = it.second.first,
            end = it.second.last
        )
    })
}