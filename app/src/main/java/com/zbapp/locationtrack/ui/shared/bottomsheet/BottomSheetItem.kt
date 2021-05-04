/*
 * Copyright (c) 2020 - Zelimir
 */

package com.zbapp.locationtrack.ui.shared.bottomsheet

import android.content.Context
import android.os.Parcelable
import android.text.SpannableString
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.utils.ListItemType
import kotlinx.parcelize.Parcelize

interface BottomSheetItemData: ListItemType, Parcelable {
    fun text(context: Context): SpannableString
}

@Parcelize
open class BottomSheetTitleData (
    val text: String
): BottomSheetItemData {
    override fun text(context: Context): SpannableString {
        return SpannableString(text)
    }

    override fun getIdentifier(): String {
        return text
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BottomSheetTitleData) return false

        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        return text.hashCode()
    }


}

@Parcelize
open class BottomSheetActionData constructor(
    val id: String? = null,
    @StringRes val text: Int ? = null,
    @DrawableRes val iconRes: Int? = null,
    @ColorRes val colorRes: Int? = null
): BottomSheetItemData {

    override fun text(context: Context): SpannableString {
        if(text != null) return SpannableString(context.getString(text))
        return SpannableString("")
    }

    override fun getIdentifier(): String {
        return id ?: text?.toString() ?: super.toString()
    }

}

object BottomSheetItems {
    val CANCEL = BottomSheetActionData("cancel", R.string.button_cancel)
}