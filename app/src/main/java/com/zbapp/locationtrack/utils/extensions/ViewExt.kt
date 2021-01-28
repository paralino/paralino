package com.zbapp.locationtrack.utils.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat


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

fun blendColors(from: Int, to: Int, ratio: Float): Int {
    val inverseRatio = 1f - ratio
    val r: Float = Color.red(to) * ratio + Color.red(from) * inverseRatio
    val g: Float = Color.green(to) * ratio + Color.green(from) * inverseRatio
    val b: Float = Color.blue(to) * ratio + Color.blue(from) * inverseRatio
    return Color.rgb(r.toInt(), g.toInt(), b.toInt())
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