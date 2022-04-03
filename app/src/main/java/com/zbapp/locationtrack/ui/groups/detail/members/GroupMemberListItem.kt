package com.zbapp.locationtrack.ui.groups.detail.members

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.utils.Emojis
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.ElementMemberListItemBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.dp2px
import com.zbapp.locationtrack.utils.extensions.fromUTCtoUserLastPing
import com.zbapp.locationtrack.utils.extensions.toLocalDateTime
import java.time.LocalDateTime

class GroupMemberListItem constructor(
    private val forViewPager: Boolean,
    context: Context,
    attributeSet: AttributeSet? = null
) : BaseRvItem<GroupMemberItemData>(context, attributeSet) {

    constructor(context: Context, attributeSet: AttributeSet? = null)
            : this(forViewPager = false, context, attributeSet)


    private val vb = ElementMemberListItemBinding.inflate(LayoutInflater.from(context), this)
    var onMemberDelete: ((GroupMemberListItem) -> Unit)? = null


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
            toggleState()
        }

        vb.bRemove.setOnClickListener {
            itemData?.let { onMemberDelete?.invoke(this) }
        }
    }

    override fun applyData(data: GroupMemberItemData) {
        vb.container.clipToOutline = true

        val userStatus = data.getUserStatus(context)
        val lastPing = data.getLastPing(context)
        vb.bAction.isVisible = data.actionItem != null
        vb.tvBatteryInfo.isVisible = data.batterLevel != null
        vb.tvUserTimeInfo.isVisible = !lastPing.isNullOrBlank()
        vb.tvUserStatus.isVisible = userStatus.isNotBlank()

        vb.bRemove.isVisible =
            data.actionItem != null && data.uiType == GroupMemberItemData.MemberUiType.EXPANDED
        data.actionItem?.let {
            vb.bRemove.setIconResource(it.actionIcon)
            vb.bRemove.setText(it.actionText)
        }

        vb.tvBatteryInfo.text = data.batterLevel?.let { "$it%" } ?: "-"
        vb.tvUserStatus.text = userStatus
        vb.tvUserTimeInfo.text = lastPing ?: ""

        vb.tvUserEmoji.text = data.emoji
        vb.tvMemberName.text = data.name

        setState()
    }

    fun toggleState() {
        if(itemData?.actionItem != null) {
            itemData?.uiType =
                itemData?.uiType?.toggle() ?: GroupMemberItemData.MemberUiType.COLLAPSED
            setState()
        }
    }

    private fun setState() {
        val data = itemData ?: return

        if (data.actionItem != null) {
            vb.bAction.animate().cancel()
            vb.bAction.animate()
                .rotation(if (data.uiType == GroupMemberItemData.MemberUiType.EXPANDED) 180f else 0f)
                .start()
        }

        vb.bRemove.isVisible =
            data.actionItem != null && data.uiType == GroupMemberItemData.MemberUiType.EXPANDED
        vb.container.elevation = when (data.uiType) {
            GroupMemberItemData.MemberUiType.COLLAPSED -> 0f.dp2px
            GroupMemberItemData.MemberUiType.EXPANDED -> 8f.dp2px
        }
    }

}

data class GroupMemberItemData constructor(
    val member: MemberModel,
    val actionItem: MemberAction? = null,
    var uiType: MemberUiType = MemberUiType.COLLAPSED
) : ListItemType {

    val name: String get() = member.name
    val emoji: String get() = member.imageBase64?.takeIf { it.isNotBlank() } ?: Emojis.robot
    val batterLevel: Int? get() = member.mutableData?.batteryLevel
    fun getLastPing(context: Context): String? {
        return member.mutableData?.timeUTC?.toLocalDateTime()?.let {
            if(it.isAfter(LocalDateTime.now().minusMinutes(2)))
                context.getString(R.string.group_members_last_active_status_now)
            else
                context.getString(R.string.group_members_last_active_status_full, it.fromUTCtoUserLastPing())
        }
    }

    fun getUserStatus(context: Context): String {
        return if (getLastPing(context).isNullOrBlank())
            context.getString(R.string.group_members_status_inactive)
        else ""
    }

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

    enum class MemberAction {
        DELETE_MEMBER, DELETE_LOCATION;

        val actionIcon: Int get() = when(this) {
            DELETE_MEMBER -> R.drawable.ic_person_remove
            DELETE_LOCATION -> R.drawable.ic_delete
        }

        val actionText: Int get() = when(this) {
            DELETE_MEMBER -> R.string.group_members_item_remove_button
            DELETE_LOCATION -> R.string.group_members_item_clear_location_button
        }
    }
}