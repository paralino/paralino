package com.zbapp.locationtrack.utils.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.PaintDrawable
import android.util.TypedValue
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import kotlin.math.max


fun View.setShapeBackgroundCircle(@ColorInt color: Int) {
    val shape = GradientDrawable()
    var size = max(height, width).takeIf { it > 0 } ?: 100
    size = size.dp2px
    shape.setSize(size, size)
    shape.shape = GradientDrawable.OVAL
    shape.color = ColorStateList.valueOf(color)
    background = shape
}

fun View.setShapeBackgroundRectangle(@ColorInt color: Int, radius: Float) {
    val shape = PaintDrawable(color)
    shape.setCornerRadius(radius)
    background = shape
}

fun View.setSelectableItemBackground() {
    with(TypedValue()) {
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
        setBackgroundResource(resourceId)
    }
}

fun TextView.setTextColorRes(@ColorRes color: Int){
    this.setTextColor(ContextCompat.getColor(this.context, color))
}

fun View.backgroundTintRes(@ColorRes resource: Int) {
    ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(ContextCompat.getColor(context, resource)))
}

fun AppCompatImageView.imageTintRes(@ColorRes resource: Int) {
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(ContextCompat.getColor(context, resource)))
}

fun View.getBitmapFromView(): Bitmap? {
    val bitmap = Bitmap.createBitmap(
        this.measuredWidth, this.measuredHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN)
    this.background?.draw(canvas)
    this.draw(canvas)
    return bitmap
}

fun View.fadeIn(finalAlpha: Float = 1f, duration: Long = 300L, delay: Long = 0, withScaleFrom: Float? = null, finished: (() -> Unit)? = null) {

    val originScaleX = scaleX
    val originScaleY = scaleY

    val animation = animate().alpha(finalAlpha)
        .setDuration(duration)
        .setStartDelay(delay)
        .setListener(object: AnimatorListenerAdapter(){
            override fun onAnimationCancel(animation: Animator?) {
                scaleX = originScaleX
                scaleY = originScaleY
                alpha = finalAlpha
            }

            override fun onAnimationEnd(animation: Animator?) {
                scaleX = originScaleX
                scaleY = originScaleY
                alpha = finalAlpha
                finished?.invoke()
            }
        })

    if(withScaleFrom != null && visibility != View.VISIBLE) {
        scaleX *= withScaleFrom
        scaleY *= withScaleFrom
        animation.scaleX(originScaleX)
        animation.scaleY(originScaleY)
        animation.interpolator = OvershootInterpolator()
    }

    if (visibility != View.VISIBLE){
        alpha = 0f
        visibility = View.VISIBLE
    }

    animation.start()
}

fun View.fadeOut(toGone: Boolean = true, finalAlpha: Float = 0f, duration: Long = 300L, delay: Long = 0, withScaleTo: Float? = null, finished: (() -> Unit)? = null)  {

    val originScaleX = scaleX
    val originScaleY = scaleY
    val originAlpha = alpha


    val animation = animate().alpha(finalAlpha)
        .setDuration(duration)
        .setStartDelay(delay)
        .setListener(object: AnimatorListenerAdapter(){
            override fun onAnimationCancel(animation: Animator?) {
                scaleX = originScaleX
                scaleY = originScaleY
                alpha = originAlpha
            }

            override fun onAnimationEnd(animation: Animator?) {
                visibility = if(toGone) View.GONE else View.INVISIBLE
                scaleX = originScaleX
                scaleY = originScaleY
                alpha = originAlpha
                finished?.invoke()
            }
        })

    if(withScaleTo != null) {
        animation.scaleX(scaleX * withScaleTo)
        animation.scaleY(scaleY * withScaleTo)
    }

    animation.start()
}

inline val Int.px2dp : Int
    get() = (this.toFloat() / Resources.getSystem().displayMetrics.density).toInt()

inline val Int.dp2px : Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

inline val Double.px2dp : Double
    get() = this / Resources.getSystem().displayMetrics.density

inline val Double.dp2px : Double
    get() = this * Resources.getSystem().displayMetrics.density

inline val Float.px2dp : Float
    get() = this / Resources.getSystem().displayMetrics.density

inline val Float.dp2px : Float
    get() = this * Resources.getSystem().displayMetrics.density
