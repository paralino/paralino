package com.zbapp.locationtrack.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.utils.extensions.bitmapFromBase64

object MapPin {

    fun populateWithBase64(parent: ViewGroup, imageBase64: String?): Bitmap? {
        val marker: View = populateView(parent)
        val markerImage: ImageView = marker.findViewById<View>(R.id.ivPinImage) as ImageView
        val b = imageBase64?.bitmapFromBase64()
        if(b != null) markerImage.setImageBitmap(b)
        else markerImage.setImageResource(R.drawable.ic_user_placeholder_round)
        return marker.getBitmapFromView()
    }

    fun populateWithResource(parent: ViewGroup, @DrawableRes resId: Int): Bitmap? {
        val marker: View = populateView(parent)
        val markerImage: ImageView = marker.findViewById<View>(R.id.ivPinImage) as ImageView
        markerImage.setImageResource(resId)
        return marker.getBitmapFromView()
    }

    private fun populateView(parent: ViewGroup): View {
        val marker: View = LayoutInflater.from(parent.context).inflate(R.layout.element_map_pin, null)
        marker.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        marker.layout(
            0,
            0,
            marker.measuredWidth,
            marker.measuredHeight
        )
        return marker
    }

    private fun View.getBitmapFromView(): Bitmap? {
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
}