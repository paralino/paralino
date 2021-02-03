/*
 * Copyright (c) 2020 - Zelimir
 */

package com.zbapp.locationtrack.ui.shared.bottomsheet

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.LayoutDialogBottomSheetBinding
import com.zbapp.locationtrack.utils.SimpleRvAdapter
import com.zbapp.locationtrack.utils.extensions.viewBinding
import com.zbapp.locationtrack.utils.withClickAction

open class BottomSheetDialog: BottomSheetDialogFragment() {

    val binding by viewBinding(LayoutDialogBottomSheetBinding::bind)

    var actionClick: ((BottomSheetActionData) -> Unit)? = null
    var actionDismiss: (() -> Unit)? = null

    private var canDismiss = true

    protected open var adapter = SimpleRvAdapter()
        .addFactory { BottomSheetActionItem(it).withClickAction { performAction(it) } }
        .addFactory { BottomSheetTitleItem(it) }

    protected open val items: List<BottomSheetItemData>
        get() = arguments?.getParcelableArrayList<BottomSheetItemData>("items") as? List<BottomSheetItemData>? ?: emptyList()

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogStyle
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_dialog_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.submitList(items)
        binding.rvBottomSheet?.adapter = adapter
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog =  super.onCreateDialog(savedInstanceState)

        if (isCancelable) dialog.setOnCancelListener { dismiss() }

        dialog.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dismiss()
                return@setOnKeyListener true
            }
            false
        }
        return dialog
    }

    protected fun performAction(item: BottomSheetActionItem){
        val data = item.itemData ?: return
        actionClick?.invoke(data)
        if(canDismiss){
            dismiss()
        }
    }

    override fun onDestroy() {
        dialog?.setOnCancelListener(null)
        dialog?.setOnDismissListener(null)
        super.onDestroy()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        actionDismiss?.invoke()
    }

    companion object {

        fun show(
            fragmentManager: FragmentManager,
            data: MutableList<BottomSheetItemData>,
            action: ((BottomSheetActionData) -> Unit)? = null
        ): BottomSheetDialog {
            val dialog = BottomSheetDialog().apply {
                arguments = bundleOf("items" to data)
            }
            dialog.actionClick = action
            dialog.show(fragmentManager, "bottomSheet")
            return dialog
        }
    }
}