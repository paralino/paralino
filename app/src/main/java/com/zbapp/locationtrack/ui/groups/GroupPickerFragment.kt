package com.zbapp.locationtrack.ui.groups

import android.os.Bundle
import android.view.View
import com.zbapp.locationtrack.ui.dashboard.GroupListItem
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetActionData
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.utils.SimpleRvAdapter
import com.zbapp.locationtrack.utils.withClickAction

class GroupPickerFragment: BottomSheetDialog() {

    override var adapter: SimpleRvAdapter = super.adapter
        .addFactory {
            GroupListItem(it).withClickAction {
                performAction(BottomSheetActionData(it.itemData?.id))
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}