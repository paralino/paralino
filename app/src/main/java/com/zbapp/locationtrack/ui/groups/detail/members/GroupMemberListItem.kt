package com.zbapp.locationtrack.ui.groups.detail.members

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.utils.Emojis
import com.zbapp.locationtrack.databinding.ElementMemberListItemBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.dp2px
import com.zbapp.locationtrack.utils.extensions.fromUTCtoUserLastPing
import com.zbapp.locationtrack.utils.extensions.toLocalDateTime

class GroupMemberListItem constructor(private val forViewPager: Boolean, context: Context, attributeSet: AttributeSet? = null)
    : BaseRvItem<GroupMemberItemData>(context, attributeSet) {

    constructor(context: Context, attributeSet: AttributeSet? = null)
            : this(forViewPager = false, context, attributeSet)


    private val vb = ElementMemberListItemBinding.inflate(LayoutInflater.from(context), this)
    var onMemberDelete: ((GroupMemberItemData) -> Unit)? = null


    init {
        if (forViewPager) {
            rootView.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        } else {
            setFullWidth()
        }

        vb.container.setOnClickListener {
            itemData?.uiType =
                itemData?.uiType?.toggle() ?: GroupMemberItemData.MemberUiType.EXPANDED
            if (itemData?.uiType == GroupMemberItemData.MemberUiType.MAP) {
                performClick()
            } else {
                setState()
            }
        }

        vb.bRemove.setOnClickListener {
            itemData?.let { onMemberDelete?.invoke(it) }
        }
    }

    override fun applyData(data: GroupMemberItemData) {
        vb.container.clipToOutline = true

        vb.bAction.isVisible = data.canDelete
        vb.tvBatteryInfo.isVisible = data.batterLevel != null
        vb.tvUserTimeInfo.isVisible = !data.lastPing.isNullOrBlank()
        vb.tvUserStatus.isVisible = data.userStatus.isNotBlank()
        vb.bRemove.isVisible = data.canDelete == true && data.uiType == GroupMemberItemData.MemberUiType.EXPANDED

        vb.tvBatteryInfo.text = data.batterLevel?.let { "$it%" } ?: "-"
        vb.tvUserStatus.text = data.userStatus
        vb.tvUserTimeInfo.text = data.lastPing ?: ""

        vb.tvUserEmoji.text = data.emoji
        vb.tvMemberName.text = data.name

//        setState()
    }

    private fun setState() {
        val type = itemData?.uiType ?: GroupMemberItemData.MemberUiType.MAP

        vb.bAction.animate().cancel()
        vb.bAction.animate()
            .rotation(if (type == GroupMemberItemData.MemberUiType.EXPANDED) 180f else 0f)
            .start()
        vb.bRemove.isVisible =
            itemData?.canDelete == true && type == GroupMemberItemData.MemberUiType.EXPANDED
        vb.container.elevation = when (type) {
            GroupMemberItemData.MemberUiType.COLLAPSED -> 0f.dp2px
            GroupMemberItemData.MemberUiType.EXPANDED -> 8f.dp2px
            GroupMemberItemData.MemberUiType.MAP -> 8f.dp2px
        }
    }

}

data class GroupMemberItemData constructor(
    val member: MemberModel,
    val canDelete: Boolean,
    var uiType: MemberUiType = MemberUiType.COLLAPSED
) : ListItemType {

    val name: String get() = member.name
    val emoji: String get() = member.imageBase64?.takeIf { it.isNotBlank() } ?: Emojis.robot
    val batterLevel: Int? get() = member.mutableData?.batteryLevel
    val lastPing: String?
        get() = member.mutableData?.timeUTC?.toLocalDateTime()?.fromUTCtoUserLastPing()
    val userStatus: String get() = if (lastPing.isNullOrBlank()) "User is inactive" else ""

    override fun getIdentifier(): String {
        return member.id
    }

    enum class MemberUiType {
        EXPANDED, COLLAPSED, MAP;

        fun toggle() = when (this) {
            EXPANDED -> COLLAPSED
            COLLAPSED -> EXPANDED
            MAP -> MAP
        }
    }
}