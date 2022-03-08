package com.zbapp.locationtrack.ui.shared.emoji

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import com.zbapp.locationtrack.databinding.ElementEmojiBinding
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetItemData
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.extensions.viewBinding
import kotlinx.parcelize.Parcelize

class EmojiItem(context: Context, attributeSet: AttributeSet? = null)
    : BaseRvItem<EmojiItemData>(context, attributeSet) {

    val vb by viewBinding(ElementEmojiBinding::inflate)

    override fun applyData(data: EmojiItemData) {
        vb.tvGroupEmoji.text = data.content
    }

    override fun setOnClickListener(l: OnClickListener?) {
        vb.tvGroupEmoji.setOnClickListener(l)
    }

}

@Parcelize
data class EmojiItemData(
    val content: String
): BottomSheetItemData {
    override fun getIdentifier(): String {
        return content
    }

    override fun text(context: Context): SpannableString {
        return SpannableString(content)
    }
}