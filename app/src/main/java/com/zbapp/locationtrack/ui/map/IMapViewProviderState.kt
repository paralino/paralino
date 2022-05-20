package com.zbapp.locationtrack.ui.map

import android.view.View
import com.zbapp.core.presentation.map.MapMarkerState

interface IMapViewProviderState {

    val mapAndroidView: View
    var onMapTouch: (() -> Boolean)?
    var onMarkerClick: ((String) -> Boolean)?

    fun initMap()

    fun updateMarkers(list: List<MapMarkerState>)
    fun reDrawMarkers(trackMarkerId: String?, trackPositions: Boolean)
    fun focusToMarker(markerId: String)
}