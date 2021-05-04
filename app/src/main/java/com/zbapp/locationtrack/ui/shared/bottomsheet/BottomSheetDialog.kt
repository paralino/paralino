/*
 * Copyright (c) 2020 - Zelimir
 */

package com.zbapp.locationtrack.ui.shared.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.LayoutDialogBottomSheetBinding
import com.zbapp.locationtrack.utils.SimpleRvAdapter
import com.zbapp.locationtrack.utils.extensions.viewBinding
import com.zbapp.locationtrack.utils.withClickAction

open class BottomSheetDialog: BottomSheetDialogFragment() {

    val binding by viewBinding(LayoutDialogBottomSheetBinding::bind)

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

        dialog.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dismissAllowingStateLoss()
                return@setOnKeyListener true
            }
            false
        }
        return dialog
    }

    protected fun performAction(item: BottomSheetActionItem){
        performAction(item.itemData ?: return)
    }

    protected fun performAction(data: BottomSheetActionData){
        setFragmentResult(keyResult, bundleOf("data" to data))
        if(canDismiss){
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroy() {
        dialog?.setOnDismissListener(null)
        super.onDestroy()
    }

    companion object {

        val keyResult = "keyResult"

        fun getAction(bundle: Bundle) = bundle.getParcelable<BottomSheetActionData>("data")

        fun show(
            parent: Fragment,
            data: MutableList<BottomSheetItemData>,
            listener: ((String, Bundle) -> Unit)? = null
        ): BottomSheetDialog {
            return show(parent, BottomSheetDialog(), data, listener)
        }

        fun show(
            parent: Fragment,
            source: BottomSheetDialog,
            data: MutableList<BottomSheetItemData>,
            listener: ((String, Bundle) -> Unit)? = null
        ): BottomSheetDialog {
            val dialog = source.apply {
                arguments = bundleOf("items" to data)
            }
            dialog.show(parent.childFragmentManager, data.hashCode().toString())

            if(listener != null) {
                parent.childFragmentManager.setFragmentResultListener(keyResult, parent, listener)
            }

            return dialog
        }

    }
}