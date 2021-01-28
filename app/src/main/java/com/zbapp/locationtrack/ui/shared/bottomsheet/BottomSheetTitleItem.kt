/*
 * Copyright (c) 2020 - Zelimir
 */

package com.zbapp.locationtrack.ui.shared.bottomsheet

import android.content.Context
import android.util.AttributeSet
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.utils.BaseRvItem
import kotlinx.android.synthetic.main.element_sheet_title.view.*

class BottomSheetTitleItem(context: Context, attributeSet: AttributeSet? = null)
    : BaseRvItem<BottomSheetTitleData>(context, attributeSet){

    init {
        inflateFullWidth(R.layout.element_sheet_title)
    }

    override fun applyData(data: BottomSheetTitleData) {
        tvTitle?.text = data.text(context)
    }

}