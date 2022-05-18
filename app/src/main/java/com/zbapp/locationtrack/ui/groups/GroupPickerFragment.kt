package com.zbapp.locationtrack.ui.groups

import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetActionData
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.utils.SimpleRvAdapter
import com.zbapp.locationtrack.utils.withClickAction

class GroupPickerFragment : BottomSheetDialog() {

    override var adapter: SimpleRvAdapter = super.adapter
        .addFactory {
            GroupListItem(it).withClickAction {
                performAction(BottomSheetActionData(it.itemData?.id))
            }
        }

}