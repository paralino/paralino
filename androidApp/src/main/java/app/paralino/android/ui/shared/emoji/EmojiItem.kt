package app.paralino.android.ui.shared.emoji

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import app.paralino.android.databinding.ElementEmojiBinding
import app.paralino.android.utils.BaseRvItem

class EmojiItem(context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<EmojiItemData>(context, attributeSet) {

    val vb = ElementEmojiBinding.inflate(LayoutInflater.from(context), this)

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