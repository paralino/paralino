package app.paralino.android.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import app.paralino.android.R
import app.paralino.android.databinding.ElementMapPinBinding
import app.paralino.android.databinding.ElementMapPinCenteredBinding
import app.paralino.android.utils.extensions.dp2px
import app.paralino.android.utils.extensions.getBitmapFromView
import app.paralino.android.utils.extensions.imageTintRes
import app.paralino.core.models.base.ImageType
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.utils.Emojis

class MapUserMarkerView : ConstraintLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    private fun inflateMemberPin(data: MapPinData) {
        val vb = ElementMapPinBinding.inflate(LayoutInflater.from(context), this, true)

        // populate data to pin view
        when (data.image?.getImageType()) {
            ImageType.emoji -> {
                vb.ivPinImage.isVisible = false
                vb.ivPinIcon.isVisible = true
                vb.ivPinIcon.setImageResource(Emojis.getResourceFromIcon(data.image!!.getImageValue()!!).drawableResId)
            }

            ImageType.placeholder, null -> {
                vb.ivPinImage.isVisible = false
                vb.ivPinIcon.isVisible = true
                vb.ivPinIcon.setImageResource(Emojis.getResourceFromIcon(Emojis.robot).drawableResId)
            }
        }

        if (data.active) {
            vb.pinBackground.imageTintRes(R.color.accentDark)
            vb.pinAnchor.setBackgroundColorRes(R.color.accentDark)
        } else {
            vb.pinBackground.imageTintRes(R.color.highlightGold)
            vb.pinAnchor.setBackgroundColorRes(R.color.highlightGold)
        }
    }

    private fun inflatePlacePin(data: MapPinData) {
        val vb = ElementMapPinCenteredBinding.inflate(LayoutInflater.from(context), this, true)

        // populate data to pin view
        when (data.image?.getImageType()) {
            ImageType.emoji -> {
                vb.ivPinImage.isVisible = false
                vb.ivPinIcon.isVisible = true
                vb.ivPinIcon.setImageResource(Emojis.getResourceFromIcon(data.image!!.getImageValue()!!).drawableResId)
            }

            ImageType.placeholder, null -> {
                vb.ivPinImage.isVisible = false
                vb.ivPinIcon.isVisible = true
                vb.ivPinIcon.setImageResource(Emojis.getResourceFromIcon(Emojis.lightBulb).drawableResId)
            }
        }

        if (data.active) {
            vb.pinBackground.setBackgroundColorRes(R.color.accentDark)
        } else {
            vb.pinBackground.setBackgroundColorRes(R.color.highlightGold)
        }
    }

    fun getPinForData(data: MapPinData): Bitmap? {

        when (data.pinType) {
            MapPinType.MEMBER -> {
                when (data.markerStyle) {
                    MapMarkerStyle.CIRCLE -> inflatePlacePin(data)
                    else -> inflateMemberPin(data)
                }
            }
            MapPinType.PLACE -> inflatePlacePin(data)
        }

        // prepare layout
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        layout(0, 0, measuredWidth, measuredHeight)

        // create bitmap from pin view and scale it down for smaller pin
        // we are doing this instead of setting dimensions directly in xml because of better rendering
        // plus it is easier to design and preview bigger view
        val bigBitmap = getBitmapFromView()
        val size = data.getPinSize().dp2px
        val small = Bitmap.createScaledBitmap(bigBitmap!!, size, size, false)

        // desaturate the pin if it is inactive
        if (!data.active) {
            val c = Canvas(small)
            val paint = Paint()
            val cm = ColorMatrix()
            cm.setSaturation(0.2f)
            val f = ColorMatrixColorFilter(cm)
            paint.colorFilter = f
            c.drawBitmap(small, 0f, 0f, paint)
        }

        return small
    }

    companion object {
        fun create(context: Context, data: MapPinData): Pair<MapPinData, BitmapDrawable> {
            val pinView = MapUserMarkerView(context)
            val bitmap = pinView.getPinForData(data)
            return data to BitmapDrawable(context.resources, bitmap)
        }
    }

}
