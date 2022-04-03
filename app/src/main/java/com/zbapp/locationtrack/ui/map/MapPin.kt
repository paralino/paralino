package com.zbapp.locationtrack.ui.map

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.utils.Emojis
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.ElementMapPinBinding
import com.zbapp.locationtrack.utils.extensions.bitmapFromBase64
import com.zbapp.locationtrack.utils.extensions.dp2px
import com.zbapp.locationtrack.utils.extensions.getBitmapFromView
import com.zbapp.locationtrack.utils.extensions.imageTintRes

class MapUserMarkerView : ConstraintLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    val vb = ElementMapPinBinding.inflate(LayoutInflater.from(context), this, true)

    fun getPinForData(data: MapPinData): Bitmap? {

        // TODO replace with different image types when implemented
        val isEmoji = true
        // populate data to pin view
        if (isEmoji) {
            vb.ivPinImage.isVisible = false
            vb.tvGroupEmoji.isVisible = true
            vb.tvGroupEmoji.text = data.image
        } else {
            vb.ivPinImage.isVisible = true
            vb.tvGroupEmoji.isVisible = false
            val b = data.image.bitmapFromBase64()
            if (b != null) vb.ivPinImage.setImageBitmap(b)
            else vb.ivPinImage.setImageResource(R.drawable.ic_user_placeholder_round)
        }

        if (data.active) {
            vb.pinBackground.imageTintRes(R.color.accentDark)
        } else {
            vb.pinBackground.imageTintRes(R.color.highlightGold)
        }

        // prepare layout
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        layout(0, 0, measuredWidth, measuredHeight)

        // create bitmap from pin view and scale it down for smaller pin
        // we are doing this instead of setting dimensions directly in xml because of better rendering
        // plus it is easier to design and preview bigger view
        val bigBitmap = getBitmapFromView()
        val size = if(data.active) 40.dp2px else 25.dp2px
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

}

data class MapPinData constructor(
    val groupId: String,
    val memberId: String,
    val focused: Boolean,
    val active: Boolean,
    val image: String
) {

    companion object {
        fun fromMember(member: MemberModel): MapPinData {
            return MapPinData(
                groupId = member.groupId,
                memberId = member.id,
                focused = true,
                active = member.isActive,
                image = member.imageBase64 ?: Emojis.robot
            )
        }
    }
}