package com.zbapp.locationtrack.ui.groups.detail.members

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.zbapp.core.models.location.MemberModel
import com.zbapp.locationtrack.databinding.ElementMemberListItemBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.dp2px

class GroupMemberListItem(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<GroupMemberItemData>(context, attributeSet) {

    private val vb = ElementMemberListItemBinding.inflate(LayoutInflater.from(context), this)
    var onMemberDelete: ((GroupMemberItemData) -> Unit)? = null

    init {
        setFullWidth()
    }

    override fun applyData(data: GroupMemberItemData) {
        vb.container.clipToOutline = true

        vb.tvUserEmoji.text = data.emoji
        vb.tvMemberName.text = data.name
        vb.bAction.isVisible = data.canDelete

        vb.container.setOnClickListener {
            itemData?.uiType = itemData?.uiType?.toggle() ?: GroupMemberItemData.MemberUiType.EXPANDED
            setState()
        }

        vb.bRemove.setOnClickListener {
            itemData?.let { onMemberDelete?.invoke(it) }
        }

        setState()
    }

    private fun setState() {
        val type = itemData?.uiType ?: GroupMemberItemData.MemberUiType.COLLAPSED

        vb.bAction.animate().cancel()
        vb.bAction.animate()
            .rotation(if (type == GroupMemberItemData.MemberUiType.EXPANDED) 180f else 0f)
            .start()
        vb.bRemove.isVisible = itemData?.canDelete == true && type == GroupMemberItemData.MemberUiType.EXPANDED
        vb.container.elevation = when (type) {
            GroupMemberItemData.MemberUiType.COLLAPSED -> 0f.dp2px
            GroupMemberItemData.MemberUiType.EXPANDED -> 2f.dp2px
        }
    }

}

data class GroupMemberItemData constructor(
    val member: MemberModel,
    val canDelete: Boolean,
    var uiType: MemberUiType = MemberUiType.COLLAPSED
) : ListItemType {

    val name: String get() = member.name
    val emoji: String get() = member.imageBase64 ?: ""

    override fun getIdentifier(): String {
        return member.id
    }

    enum class MemberUiType {
        EXPANDED, COLLAPSED;

        fun toggle() = when (this) {
            EXPANDED -> COLLAPSED
            COLLAPSED -> EXPANDED
        }
    }
}