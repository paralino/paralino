package com.zbapp.locationtrack.ui.shared.list

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.PhonelinkSetup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.isVisible
import com.zbapp.core.utils.ConditionErrorType
import com.zbapp.core.utils.LocationConditionStatus
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.ElementBannerContainerBinding
import com.zbapp.locationtrack.databinding.ElementDashboardBannerBinding
import com.zbapp.locationtrack.ui.theme.AppTheme
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.extensions.viewBinding
import com.zbapp.locationtrack.utils.withDataOnClickAction


@Composable
fun ConditionErrorItem(
    modifier: Modifier = Modifier,
    data: ConditionErrorType,
    onClick: () -> Unit
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.error)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {

        val (icon, text, action) = createRefs()

        val iconVector: ImageVector = when (data) {
            ConditionErrorType.GPS -> Icons.Rounded.LocationOff
            ConditionErrorType.PERMISSION_LOCATION -> Icons.Rounded.PhonelinkSetup
            ConditionErrorType.PERMISSION_LOCATION_PRECISE -> Icons.Rounded.PhonelinkSetup
            ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> Icons.Rounded.PhonelinkSetup
            ConditionErrorType.NETWORK -> Icons.Rounded.LocationOff
            ConditionErrorType.EMAIL_VERIFICATION -> Icons.Rounded.Email
        }
        Icon(
            imageVector = iconVector,
            modifier = Modifier
                .size(16.dp)
                .constrainAs(icon) {
                    centerVerticallyTo(parent)
                    start.linkTo(parent.start)
                },
            tint = MaterialTheme.colors.onError,
            contentDescription = null
        )

        Text(
            text = stringResource(id = data.nameRes),
            color = MaterialTheme.colors.onError,
            modifier = Modifier.constrainAs(text) {
                centerVerticallyTo(parent)
                linkTo(
                    start = icon.end,
                    end = action.start,
                    startMargin = 8.dp,
                    endMargin = 8.dp,
                    endGoneMargin = 0.dp,
                    bias = 0f
                )
                width = Dimension.preferredWrapContent
            }
        )

        if (data.endActionTextRes != 0) {
            Text(
                text = stringResource(id = data.endActionTextRes),
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .constrainAs(action) {
                        centerVerticallyTo(parent)
                        end.linkTo(parent.end)
                    }
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(vertical = 4.dp, horizontal = 12.dp)
            )
        }
    }
}

@Preview(widthDp = 320, heightDp = 200)
@Preview(widthDp = 320, heightDp = 200, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ErrorItemPreview() {
    AppTheme {
        Surface {
            Column {
                ConditionErrorItem(data = ConditionErrorType.PERMISSION_LOCATION) {}
                ConditionErrorItem(data = ConditionErrorType.NETWORK) {}
            }
        }
    }
}

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
    constructor(status: LocationConditionStatus) : this(status.getPossibleErrors())
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