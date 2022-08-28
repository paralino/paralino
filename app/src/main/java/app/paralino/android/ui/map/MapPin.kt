package app.paralino.android.ui.map

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.ImageType
import app.paralino.core.presentation.map.MapMarkerState
import app.paralino.core.utils.Emojis
import app.paralino.android.R
import app.paralino.android.databinding.ElementMapPinBinding
import app.paralino.android.utils.extensions.bitmapFromBase64
import app.paralino.android.utils.extensions.dp2px
import app.paralino.android.utils.extensions.getBitmapFromView
import app.paralino.android.utils.extensions.imageTintRes

class MapUserMarkerView : ConstraintLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    val vb = ElementMapPinBinding.inflate(LayoutInflater.from(context), this, true)

    fun getPinForData(data: MapPinData): Bitmap? {

        // populate data to pin view
        when (data.image?.getImageType()) {
            ImageType.emoji -> {
                vb.ivPinImage.isVisible = false
                vb.tvGroupEmoji.isVisible = true
                vb.tvGroupEmoji.text = data.image.getImageValue()
            }
            ImageType.raw -> {
                vb.ivPinImage.isVisible = true
                vb.tvGroupEmoji.isVisible = false
                val b = data.image.getImageValue()?.bitmapFromBase64()
                if (b != null) vb.ivPinImage.setImageBitmap(b)
                else vb.ivPinImage.setImageResource(R.drawable.ic_user_placeholder_round)
            }
            else -> {
                vb.ivPinImage.isVisible = false
                vb.tvGroupEmoji.isVisible = true
                vb.tvGroupEmoji.text = Emojis.robot
            }
        }

        if (data.active) {
            vb.pinBackground.imageTintRes(R.color.accentDark)
            vb.pinAnchor.setBackgroundColorRes(R.color.accentDark)
        } else {
            vb.pinBackground.imageTintRes(R.color.highlightGold)
            vb.pinAnchor.setBackgroundColorRes(R.color.highlightGold)
        }

        // prepare layout
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        layout(0, 0, measuredWidth, measuredHeight)

        // create bitmap from pin view and scale it down for smaller pin
        // we are doing this instead of setting dimensions directly in xml because of better rendering
        // plus it is easier to design and preview bigger view
        val bigBitmap = getBitmapFromView()
        val size = when {
            data.focused -> 60.dp2px
            data.active -> 40.dp2px
            else -> 25.dp2px
        }
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
        fun create(context: Context, model: MapMarkerState): Pair<MapPinData, BitmapDrawable> {
            val pinView = MapUserMarkerView(context)
            val data = MapPinData(model)
            val bitmap = pinView.getPinForData(data)
            return data to BitmapDrawable(context.resources, bitmap)
        }
    }

}

data class MapPinData constructor(
    val groupId: String,
    val memberId: String,
    val focused: Boolean,
    val active: Boolean,
    val image: ImageModel?,
) {

    constructor(state: MapMarkerState) : this(
        groupId = state.groupId,
        memberId = state.memberData.id,
        focused = state.focused,
        active = state.active,
        image = state.image
    )
}