package com.zbapp.locationtrack.ui.shared.list

import android.content.Context
import android.util.AttributeSet
import com.zbapp.locationtrack.databinding.ElementListSubtitleBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.viewBinding

class ListSubtitleItem(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<ListSubtitleItemData>(context, attributeSet) {

    private val binding by viewBinding(ElementListSubtitleBinding::inflate)

    override fun applyData(data: ListSubtitleItemData) {
        binding.tvSubtitle.text = data.content
    }
}

data class ListSubtitleItemData(
    val content: String
) : ListItemType {
    override fun getIdentifier(): String {
        return content
    }
}
