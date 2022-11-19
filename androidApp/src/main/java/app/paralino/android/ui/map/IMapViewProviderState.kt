package app.paralino.android.ui.map

import android.view.View
import app.paralino.core.presentation.map.MapPinData

interface IMapViewProviderState {

    val mapAndroidView: View
    var onMapTouch: (() -> Boolean)?
    var onMapTouchEnd: (() -> Unit)?
    var onMarkerClick: ((MapPinData) -> Boolean)?

    fun initMap()

    fun reDrawMarkers(trackMarkerId: String?, trackPositions: Boolean)
    fun focusToMarker(markerId: String)
}