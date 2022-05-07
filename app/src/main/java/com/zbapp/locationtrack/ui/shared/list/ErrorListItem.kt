package com.zbapp.locationtrack.ui.shared.list

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.zbapp.core.utils.ConditionErrorType
import com.zbapp.core.utils.LocationConditionStatus
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.ElementBannerContainerBinding
import com.zbapp.locationtrack.databinding.ElementDashboardBannerBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.extensions.viewBinding
import com.zbapp.locationtrack.utils.withDataOnClickAction

class ErrorListItem(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<ConditionErrorType>(context, attributeSet) {

    val vb by viewBinding(ElementDashboardBannerBinding::inflate)

    init {
        setFullWidth()
        vb.errorItemParent.clipToOutline = true
    }

    override fun applyData(data: ConditionErrorType) {
        vb.ivBannerIcon.setImageResource(data.iconRes)
        vb.tvBannerTitle.setText(data.nameRes)

        vb.tvBannerInfo.isVisible = data.endActionTextRes != 0
        data.endActionTextRes.takeIf { it != 0 }?.let { vb.tvBannerInfo.setText(it) }
    }

}

class ErrorListItemContainer(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<ErrorItems>(context, attributeSet) {

    val vb by viewBinding(ElementBannerContainerBinding::inflate)
    var itemClick: ((ConditionErrorType) -> Unit)? = null

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
    val list: List<ConditionErrorType>
) {
    constructor(status: LocationConditionStatus): this(status.getPossibleErrors())
}

val ConditionErrorType.iconRes: Int
    get() = when (this) {
        ConditionErrorType.GPS -> R.drawable.ic_location_off
        ConditionErrorType.PERMISSION_LOCATION -> R.drawable.ic_app_settings
        ConditionErrorType.PERMISSION_LOCATION_PRECISE -> R.drawable.ic_app_settings
        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> R.drawable.ic_app_settings
        ConditionErrorType.NETWORK -> R.drawable.ic_location_off
        ConditionErrorType.EMAIL_VERIFICATION -> R.drawable.ic_email
    }

val ConditionErrorType.nameRes: Int
    get() = when (this) {
        ConditionErrorType.GPS -> R.string.location_banner_error_gps_off
        ConditionErrorType.PERMISSION_LOCATION -> R.string.location_banner_error_no_permission
        ConditionErrorType.PERMISSION_LOCATION_PRECISE -> R.string.location_banner_error_no_permission
        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> R.string.location_banner_error_no_background_permission
        ConditionErrorType.NETWORK -> R.string.location_banner_error_offline
        ConditionErrorType.EMAIL_VERIFICATION -> R.string.info_email_confirm
    }

val ConditionErrorType.endActionTextRes: Int
    get() = when (this) {
        ConditionErrorType.GPS -> R.string.location_banner_fix
        ConditionErrorType.PERMISSION_LOCATION -> R.string.location_banner_fix
        ConditionErrorType.PERMISSION_LOCATION_PRECISE -> R.string.location_banner_fix
        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> R.string.location_banner_fix
        ConditionErrorType.NETWORK -> 0
        ConditionErrorType.EMAIL_VERIFICATION -> 0
    }