package com.zbapp.locationtrack.ui.shared.emoji

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.GridLayoutManager
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetActionData
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.utils.SimpleRvAdapter
import com.zbapp.locationtrack.utils.extensions.dp2px
import com.zbapp.locationtrack.utils.withClickAction

class EmojiPickerFragment: BottomSheetDialog() {

    override var adapter: SimpleRvAdapter = super.adapter
        .addFactory {
            EmojiItem(it).withClickAction {
                performAction(BottomSheetActionData(it.itemData?.content))
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvBottomSheet.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvBottomSheet.updatePaddingRelative(start = 12.dp2px, end = 12.dp2px)
    }

}