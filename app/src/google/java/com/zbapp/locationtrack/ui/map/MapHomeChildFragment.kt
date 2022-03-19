package com.zbapp.locationtrack.ui.map

import android.os.Bundle
import android.view.View
import com.zbapp.core.models.location.MemberModel
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.LayoutMapMapboxBinding
import com.zbapp.locationtrack.utils.extensions.viewBinding

class MapHomeChildFragment: IMapHomeFragment(R.layout.layout_map_mapbox) {

    private val vb by viewBinding(LayoutMapMapboxBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun updateMarkers(list: List<MemberModel>) {
        TODO("Not yet implemented")
    }

    override fun reDrawMarkers() {
        TODO("Not yet implemented")
    }

    override fun focusToMarker(markerId: String) {
        TODO("Not yet implemented")
    }
}