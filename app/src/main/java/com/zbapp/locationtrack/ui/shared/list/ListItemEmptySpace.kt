package com.zbapp.locationtrack.ui.shared.list

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.zbapp.locationtrack.utils.BaseRvItem
import com.zbapp.locationtrack.utils.ListItemType

class ListItemEmptySpace(context: Context): BaseRvItem<ListItemEmptySpaceData>(context, null) {

    override fun applyData(data: ListItemEmptySpaceData) {
        layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            data.spaceInPx
        )
    }
}

data class ListItemEmptySpaceData(
    val spaceInPx: Int
): ListItemType