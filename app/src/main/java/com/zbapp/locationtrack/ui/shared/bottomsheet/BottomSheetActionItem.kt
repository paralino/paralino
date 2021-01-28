/*
 * Copyright (c) 2020 - Zelimir
 */

package com.zbapp.locationtrack.ui.shared.bottomsheet

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.extensions.setSelectableItemBackground
import com.zbapp.locationtrack.utils.extensions.setTextColorRes
import kotlinx.android.synthetic.main.element_sheet_action.view.*

class BottomSheetActionItem(context: Context, attributeSet: AttributeSet? = null)
    : BaseRvItem<BottomSheetActionData>(context, attributeSet){

    init {
        inflateFullWidth(R.layout.element_sheet_action)
        setSelectableItemBackground()
    }

    override fun applyData(data: BottomSheetActionData) {
        ivIcon?.isVisible = data.iconRes != null
        data.iconRes?.let { ivIcon?.setImageResource(it) }

        tvTitle?.text = data.text(context)

        if(data.colorRes != null) {
            tvTitle?.setTextColorRes(data.colorRes)
        } else {
            tvTitle?.setTextColorRes(R.color.primaryTextColor)
        }

    }

}