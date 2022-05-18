package com.zbapp.locationtrack.ui.groups

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.base.ImageModel
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.data.mappers.ImageModelParceler
import com.zbapp.locationtrack.databinding.ElementGroupListItemBinding
import com.zbapp.locationtrack.ui.components.ImageModelImage
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetItemData
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.dp2px
import com.zbapp.locationtrack.utils.extensions.safeCast
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

class GroupListItem(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<GroupItemData>(context, attributeSet) {

    private val vb = ElementGroupListItemBinding.inflate(LayoutInflater.from(context), this)
    var onGroupActionClick: ((GroupItemData) -> Unit)? = null

    init {
        setFullWidth()
    }

    override fun applyData(data: GroupItemData) {
        vb.container.clipToOutline = true
        vb.container.elevation = when (data.uiType) {
            GroupItemData.GroupUiType.ACTIVE -> 8f.dp2px
            GroupItemData.GroupUiType.INACTIVE -> 2f.dp2px
            else -> 0f.dp2px
        }
        vb.container.layoutParams.safeCast<MarginLayoutParams>()?.bottomMargin = when(data.uiType) {
            GroupItemData.GroupUiType.PICKER -> 0
            else -> context.resources.getDimensionPixelSize(R.dimen.activity_side_margin_medium)
        }
        vb.tvGroupEmoji.text = data.image.getImageValue()
        vb.tvGroupName.text = data.name
        vb.tvGroupCount.isVisible = data.userCount != null
        vb.tvGroupCount.text = data.userCount?.toString() ?: "-"
        vb.bAction.isVisible = data.uiType == GroupItemData.GroupUiType.ACTIVE
        vb.bAction.setOnClickListener {
            itemData?.let { onGroupActionClick?.invoke(it) }
        }
    }

}

@Parcelize
data class GroupItemData constructor(
    val id: String,
    val name: String,
    val image: @WriteWith<ImageModelParceler>() ImageModel,
    val userCount: Int?,
    val uiType: GroupUiType = GroupUiType.INACTIVE
) : ListItemType, BottomSheetItemData {

    constructor(group: UserGroup, uiType: GroupUiType): this(
        group.id, group.name, group.image, group.memberCount, uiType
    )

    override fun getIdentifier(): String {
        return id
    }

    override fun text(context: Context): SpannableString {
        return SpannableString(name)
    }

    enum class GroupUiType {
        ACTIVE, INACTIVE, PICKER
    }
}

@Composable
fun GroupListItem(
    data: com.zbapp.core.presentation.dashboard.GroupItemData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageModelImage(
            imageModel = data.image,
            modifier = Modifier
                .size(50.dp)
                .border(
                    0.5.dp,
                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                    RoundedCornerShape(8.dp)
                )
        )

        Spacer(modifier = Modifier.size(16.dp))
        Column(
            modifier = Modifier
        ) {
            Text(
                text = data.name,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier
            )

            if (data.userCount != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            modifier = Modifier.size(16.dp),
                            contentDescription = null
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = "${data.userCount}",
                            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}