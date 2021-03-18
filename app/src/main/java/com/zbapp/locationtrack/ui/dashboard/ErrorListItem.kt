package com.zbapp.locationtrack.ui.dashboard

import android.content.Context
import android.util.AttributeSet
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.ElementDashboardBannerBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.viewBinding

class ErrorListItem(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<ErrorListItemType>(context, attributeSet) {

    val vb by viewBinding(ElementDashboardBannerBinding::inflate)

    init {
        setFullWidth()
        vb.root.clipToOutline = true
    }

    override fun applyData(data: ErrorListItemType) {
        vb.ivBannerIcon.setImageResource(data.iconRes)
        vb.tvBannerTitle.setText(data.nameRes)
    }

}

enum class ErrorListItemType : ListItemType {

    GPS,
    PERMISSION;

    val iconRes: Int
        get() = when (this) {
            GPS -> R.drawable.ic_location_off
            PERMISSION -> R.drawable.ic_app_settings
        }

    val nameRes: Int
        get() = when (this) {
            GPS -> R.string.location_banner_error_gps_off
            PERMISSION -> R.string.location_banner_error_no_permission
        }

    override fun getIdentifier(): String {
        return this.name
    }

}