package app.paralino.android.ui.shared.emoji

import android.content.Context
import android.util.AttributeSet
import app.paralino.android.databinding.ElementEmojiBinding
import app.paralino.android.utils.BaseRvItem
import app.paralino.android.utils.extensions.viewBinding

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