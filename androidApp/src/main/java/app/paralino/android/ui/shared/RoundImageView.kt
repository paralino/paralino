/*
 * Copyright (c) 2020 - Zelimir
 */

package app.paralino.android.ui.shared

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import app.paralino.android.R
import app.paralino.android.utils.extensions.setShapeBackgroundCircle
import app.paralino.android.utils.extensions.setShapeBackgroundRectangle

class RoundImageView: AppCompatImageView {

    private var _backgroundColor: Int = 0
    private var _radius: Float = -1f

    constructor(context: Context) : super(context){
        _backgroundColor = context.getColor(R.color.backgroundMain)
        _radius = -1f
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs){
        initAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initAttributes(attrs)
    }

    init {
        clipToOutline = true
        adjustViewBounds = true
        scaleType = ScaleType.CENTER_CROP
    }

    private fun initAttributes(attrs: AttributeSet?) {
        if(attrs != null) {
            val attributes = context.obtainStyledAttributes(
                attrs, R.styleable.RoundImageView, 0, 0
            )

            _backgroundColor = attributes.getColor(R.styleable.RoundImageView_backgroundColor, context.getColor(R.color.backgroundMain))
            _radius = attributes.getDimension(R.styleable.RoundImageView_cornerRadius, -1f)
            applyAttributes()
            attributes.recycle()
        }
    }

    override fun setBackgroundColor(color: Int) {
        _backgroundColor = color
        applyAttributes()
    }

    fun setBackgroundColorRes(@ColorRes colorRes: Int) {
        _backgroundColor = ContextCompat.getColor(context, colorRes)
        applyAttributes()
    }

    fun setRadius(radius: Float) {
        _radius = radius
        applyAttributes()
    }

    private fun applyAttributes() {
        when {
            _radius > 0 -> setShapeBackgroundRectangle(_backgroundColor, _radius)
            _radius < 0 -> setShapeBackgroundCircle(_backgroundColor)
            else -> setBackgroundColor(_backgroundColor)
        }
    }

}