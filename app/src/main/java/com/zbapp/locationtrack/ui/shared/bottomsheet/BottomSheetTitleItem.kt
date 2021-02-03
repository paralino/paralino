/*
 * Copyright (c) 2020 - Zelimir
 */

package com.zbapp.locationtrack.ui.shared.bottomsheet

import android.content.Context
import android.util.AttributeSet
import com.zbapp.locationtrack.databinding.ElementSheetTitleBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.extensions.viewBinding

class BottomSheetTitleItem(context: Context, attributeSet: AttributeSet? = null)
    : BaseRvItem<BottomSheetTitleData>(context, attributeSet){

    val binding by viewBinding(ElementSheetTitleBinding::inflate)

    init {
        setFullWidth()
    }

    override fun applyData(data: BottomSheetTitleData) {
        binding.tvTitle?.text = data.text(context)
    }

}