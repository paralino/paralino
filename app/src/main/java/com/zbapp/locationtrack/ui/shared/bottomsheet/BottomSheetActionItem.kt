/*
 * Copyright (c) 2020 - Zelimir
 */

package com.zbapp.locationtrack.ui.shared.bottomsheet

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.ElementSheetActionBinding
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.extensions.setSelectableItemBackground
import com.zbapp.locationtrack.utils.extensions.setTextColorRes
import com.zbapp.locationtrack.utils.extensions.viewBinding

class BottomSheetActionItem(context: Context, attributeSet: AttributeSet? = null)
    : BaseRvItem<BottomSheetActionData>(context, attributeSet){

    val binding by viewBinding(ElementSheetActionBinding::inflate)

    init {
        setFullWidth()
        setSelectableItemBackground()
    }

    override fun applyData(data: BottomSheetActionData) {
        binding.ivIcon?.isVisible = data.iconRes != null
        data.iconRes?.let { binding.ivIcon?.setImageResource(it) }

        binding.tvTitle?.text = data.text(context)

        if(data.colorRes != null) {
            binding.tvTitle?.setTextColorRes(data.colorRes)
        } else {
            binding.tvTitle?.setTextColorRes(R.color.primaryTextColor)
        }

    }

}