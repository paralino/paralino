package app.paralino.android.ui.map

import android.view.View
import app.paralino.core.presentation.map.MapMarkerState

interface IMapViewProviderState {

    val mapAndroidView: View
    var onMapTouch: (() -> Boolean)?
    var onMarkerClick: ((String) -> Boolean)?

    fun initMap()

    fun updateMarkers(list: List<MapMarkerState>)
    fun reDrawMarkers(trackMarkerId: String?, trackPositions: Boolean)
    fun focusToMarker(markerId: String)
}