package com.zbapp.locationtrack.ui.shared.list

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.ElementBannerContainerBinding
import com.zbapp.locationtrack.databinding.ElementDashboardBannerBinding
import com.zbapp.locationtrack.tracking.LocationConditionStatus
import com.zbapp.locationtrack.utils.*
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

        vb.tvBannerInfo.isVisible = data.endActionTextRes != 0
        data.endActionTextRes.takeIf { it != 0 }?.let { vb.tvBannerInfo.setText(it) }
    }

}

class ErrorListItemContainer(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<ErrorItems>(context, attributeSet) {

    val vb by viewBinding(ElementBannerContainerBinding::inflate)
    var itemClick: ((ErrorListItemType) -> Unit)? = null

    init {
        setFullWidth()
    }

    override fun applyData(data: ErrorItems) {
        vb.root.removeAllViews()
        data.list.forEach { error ->
            val item = ErrorListItem(context).apply {
                itemData = error
            }.withDataOnClickAction(itemClick)
            vb.root.addView(item)
        }
    }

}

data class ErrorItems(
    val list: List<ErrorListItemType>
) {
    constructor(status: LocationConditionStatus): this(
        mutableListOf<ErrorListItemType>().apply {
            if(!status.isPermissionAllowed) add(ErrorListItemType.PERMISSION)
            if(!status.isGpsOn) add(ErrorListItemType.GPS)
        }.toList()
    )
}

enum class ErrorListItemType : ListItemType {

    GPS,
    PERMISSION,
    EMAIL;

    val iconRes: Int
        get() = when (this) {
            GPS -> R.drawable.ic_location_off
            PERMISSION -> R.drawable.ic_app_settings
            EMAIL -> R.drawable.ic_email
        }

    val nameRes: Int
        get() = when (this) {
            GPS -> R.string.location_banner_error_gps_off
            PERMISSION -> R.string.location_banner_error_no_permission
            EMAIL -> R.string.info_email_confirm
        }

    val endActionTextRes: Int
        get() = when (this) {
            GPS -> R.string.location_banner_fix
            PERMISSION -> R.string.location_banner_fix
            EMAIL -> 0
        }

    override fun getIdentifier(): String {
        return this.name
    }

}