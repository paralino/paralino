package com.zbapp.locationtrack.ui.map

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.fragment.app.Fragment
import com.zbapp.core.models.location.MemberModel

abstract class IMapHomeFragment(layoutRes: Int): Fragment(layoutRes) {

    open var onMapTouch: (() -> Boolean)? = null
    open var onMarkerClick: ((String) -> Boolean)? = null

    /**
     * focus and track all markers
     */
    var trackPositions: Boolean = true

    /**
     * id of specific marker to track and focus on
     */
    var trackMarkerId: String? = null

    abstract fun updateMarkers(list: List<MemberModel>)
    abstract fun reDrawMarkers()
    abstract fun focusToMarker(markerId: String)

    fun getPin(context: Context, model: MemberModel): Pair<MapPinData, BitmapDrawable> {
        val pinView = MapUserMarkerView(context)
        val data = MapPinData.fromMember(model)
        val bitmap = pinView.getPinForData(data)
        return data to BitmapDrawable(requireContext().resources, bitmap)
    }
}

