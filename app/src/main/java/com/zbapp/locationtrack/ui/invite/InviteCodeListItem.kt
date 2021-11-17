package com.zbapp.locationtrack.ui.invite

import android.content.Context
import android.util.AttributeSet
import com.zbapp.core.models.invite.InviteCode
import com.zbapp.locationtrack.databinding.ElementInviteCodeBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.toDateTimeUTC
import com.zbapp.locationtrack.utils.extensions.toLocalString
import com.zbapp.locationtrack.utils.extensions.viewBinding
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class InviteCodeListItem(context: Context, attributeSet: AttributeSet? = null)
    : BaseRvItem<InviteCodeData>(context, attributeSet) {

    private val vb by viewBinding(ElementInviteCodeBinding::inflate)
    var onCopy: ((InviteCodeData) -> Unit)? = null
    var onShare: ((InviteCodeData) -> Unit)? = null
    var onDelete: ((InviteCodeData) -> Unit)? = null

    init {
        setFullWidth()
        vb.bCopy.setOnClickListener { itemData?.let { onCopy?.invoke(it) } }
        vb.bShare.setOnClickListener { itemData?.let { onShare?.invoke(it) } }
        vb.bDelete.setOnClickListener { itemData?.let { onDelete?.invoke(it) } }
    }

    override fun applyData(data: InviteCodeData) {
        vb.tvInviteCode.text = data.invite.inviteCode
        vb.tvInviteCodeValidity.text = data.invite.validity.toDateTimeUTC().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT))
    }
}

data class InviteCodeData(
    val invite: InviteCode
): ListItemType {
    override fun getIdentifier(): String {
        return invite.inviteCode
    }
}