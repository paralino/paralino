package com.zbapp.locationtrack.ui.shared.list

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.zbapp.locationtrack.databinding.ElementGenericInfoBinding
import com.zbapp.locationtrack.ui.shared.ButtonWithProgress
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.viewBinding

class ListItemEmptyState (context: Context, attributeSet: AttributeSet? = null) :
    BaseRvItem<EmptyStateData>(context, attributeSet) {

    private val vb by viewBinding(ElementGenericInfoBinding::inflate)

    init {
        setFullWidth()
    }

    override fun applyData(data: EmptyStateData) {
        vb.tvInfo.isVisible = data.infoText.isNotBlank()
        vb.ivInfo.isVisible = data.iconRes?.takeIf { it != 0 } != null
        vb.bAction.isVisible = data.buttonText.isNotBlank()

        vb.tvInfo.text = data.infoText
        vb.bAction.setContent(data.buttonText)
        vb.ivInfo.setImageResource(data.iconRes ?: 0)
    }

    fun getButton(): ButtonWithProgress {
        return vb.bAction
    }

    override fun setOnClickListener(l: OnClickListener?) {
        vb.bAction.setOnClickListener(l)
    }
}

data class EmptyStateData constructor(
    @DrawableRes val iconRes: Int? = null,
    val infoText: String = "",
    val buttonText: String = ""
): ListItemType {
    override fun getIdentifier(): String {
        return infoText
    }
}

val RecyclerView.emptyStateItem: ListItemEmptyState? get() {
    return this.children.firstOrNull { it is ListItemEmptyState } as? ListItemEmptyState
}