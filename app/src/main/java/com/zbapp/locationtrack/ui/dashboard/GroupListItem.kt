package com.zbapp.locationtrack.ui.dashboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.zbapp.core.models.group.UserGroup
import com.zbapp.locationtrack.databinding.ElementGroupListItemBinding
import com.zbapp.locationtrack.databinding.ElementProgressButtonBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.dp2px
import com.zbapp.locationtrack.utils.extensions.viewBinding

class GroupListItem(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<GroupItemData>(context, attributeSet) {

    private val vb = ElementGroupListItemBinding.inflate(LayoutInflater.from(context), this)
    var onGroupActionClick: ((GroupItemData) -> Unit)? = null

    init {
        setFullWidth()
    }

    override fun applyData(data: GroupItemData) {
        vb.container.clipToOutline = true
        vb.container.elevation = if(data.active) 8f.dp2px else 2f.dp2px
        vb.tvGroupEmoji.text = data.group.emoji
        vb.tvGroupName.text = data.group.name
        vb.tvGroupCount.text = data.group.userIds.size.toString()
        vb.bAction.isVisible = data.active
        vb.bAction.setOnClickListener {
            itemData?.let { onGroupActionClick?.invoke(it) }
        }
    }

}

data class GroupItemData(
    val group: UserGroup,
    val active: Boolean
) : ListItemType {

    override fun getIdentifier(): String {
        return group.id
    }

}