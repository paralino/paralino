package com.zbapp.locationtrack.ui.dashboard

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.zbapp.core.models.group.UserGroup
import com.zbapp.locationtrack.databinding.ElementGroupListItemBinding
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetItemData
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.dp2px
import kotlinx.parcelize.Parcelize

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
        vb.tvGroupEmoji.text = data.emoji
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
    val emoji: String,
    val userCount: Int?,
    val uiType: GroupUiType = GroupUiType.INACTIVE
) : ListItemType, BottomSheetItemData {

    constructor(group: UserGroup, uiType: GroupUiType): this(
        group.id, group.name, group.emoji, group.members.size, uiType
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