package com.zbapp.locationtrack.ui.dashboard

import android.content.Context
import android.util.AttributeSet
import com.zbapp.locationtrack.databinding.ElementGroupsAddBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.viewBinding

class GroupAddItem(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<GroupAddData>(context, attributeSet) {

    val vb by viewBinding(ElementGroupsAddBinding::inflate)

    init {
        setFullWidth()
        vb.tvContent.clipToOutline = true
    }
    override fun applyData(data: GroupAddData) { }

    override fun setOnClickListener(l: OnClickListener?) {
        vb.tvContent.setOnClickListener(l)
    }
}

data class GroupAddData(
    val id: String = "group_add"
) : ListItemType {

    override fun getIdentifier(): String {
        return id
    }

}