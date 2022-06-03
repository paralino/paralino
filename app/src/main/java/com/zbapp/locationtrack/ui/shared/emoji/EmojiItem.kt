package com.zbapp.locationtrack.ui.shared.emoji

import android.content.Context
import android.util.AttributeSet
import com.zbapp.locationtrack.databinding.ElementEmojiBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.extensions.viewBinding

class EmojiItem(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<EmojiItemData>(context, attributeSet) {

    val vb by viewBinding(ElementEmojiBinding::inflate)

    override fun applyData(data: EmojiItemData) {
        vb.tvGroupEmoji.text = data.content
    }

    override fun setOnClickListener(l: OnClickListener?) {
        vb.tvGroupEmoji.setOnClickListener(l)
    }

}

data class EmojiItemData(
    val content: String,
) {
    fun getIdentifier(): String {
        return content
    }
}