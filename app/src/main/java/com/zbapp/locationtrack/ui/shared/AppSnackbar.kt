/*
 * Copyright (c) 2020 - Zelimir
 */

package com.zbapp.locationtrack.ui.shared

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.ContentViewCallback
import com.google.android.material.snackbar.Snackbar
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.ItemSnackbarBinding
import com.zbapp.locationtrack.utils.extensions.attributedBold
import com.zbapp.locationtrack.utils.extensions.fadeIn
import com.zbapp.locationtrack.utils.extensions.fadeOut
import com.zbapp.locationtrack.utils.extensions.viewBinding

class AppSnackbarView: ConstraintLayout, ContentViewCallback {

    var actionClick : (() -> Unit)? = null
    val binding: ItemSnackbarBinding by viewBinding(ItemSnackbarBinding::inflate)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)


    init {
        clipToPadding = false
        binding.bAction.setOnClickListener {
            actionClick?.invoke()
        }
    }

    fun dataReady(data: AppSnackbarData) {
        binding.tvText?.text = data.text
        binding.bAction?.isVisible = !data.actionText.isNullOrBlank()
        binding.bAction?.text = data.actionText ?: ""
    }

    override fun animateContentOut(delay: Int, duration: Int) {
        binding.snackbarHolder?.fadeOut(withScaleTo = 0.6f)
    }

    override fun animateContentIn(delay: Int, duration: Int) {
        binding.snackbarHolder?.fadeIn(withScaleFrom = 0.6f)
    }
}

data class AppSnackbarData constructor(
    val text: SpannableString,
    val actionText: String? = null,
    val duration: Int = if(actionText.isNullOrBlank()) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_INDEFINITE,
    val id: String = text.toString()
){
    constructor(
        text: String,
        actionText: String?,
        duration: Int = if(actionText.isNullOrBlank()) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_INDEFINITE,
        id: String = text
    ): this(SpannableString(text), actionText, duration, id)
}

class AppSnackbar (
    parent: ViewGroup,
    content: AppSnackbarView
) : BaseTransientBottomBar<AppSnackbar>(parent, content, content) {


    init {
        getView().setBackgroundResource(R.color.transparent)
        getView().setPadding(0, 0, 0, 0)
    }

    companion object {

        fun make(anchorView: View?, data: AppSnackbarData, actionClick : (() -> Unit)? = null): AppSnackbar? {
            val parent = anchorView.findSnackbarParent() ?: throw IllegalArgumentException(
                "Provide a view with a valid parent."
            )

            try{
                val snackbar = AppSnackbarView(anchorView?.context ?: return null, null)
                snackbar.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                snackbar.actionClick = actionClick
                snackbar.dataReady(data)
                return AppSnackbar(parent, snackbar)
                    .setDuration(data.duration)
                    .setAnimationMode(ANIMATION_MODE_SLIDE)
            }catch ( e: Exception){ }

            return null
        }

    }

}

fun snackBarSimple(
    anchorView: View?,
    content: String,
    actionText: String? = null,
    actionClick: (() -> Unit)? = null
): AppSnackbar? {
    val context = anchorView?.context ?: return null
    return snackBarSimple(anchorView, content.attributedBold(context), actionText, actionClick)
}

fun snackBarSimple(
    anchorView: View?,
    content: SpannableString,
    actionText: String? = null,
    actionClick: (() -> Unit)? = null
): AppSnackbar? {
    return AppSnackbar.make(
        anchorView,
        AppSnackbarData(content, actionText),
        actionClick
    )?.also { it.show() }
}

fun snackBarError(
    anchorView: View?,
    error: Throwable? = null,
    actionText: String? = null,
    actionClick: (() -> Unit)? = null
): AppSnackbar? {
    val context = anchorView?.context ?: return null
    return snackBarSimple(anchorView, context.errorString(error), actionText, actionClick)
}

internal fun View?.findSnackbarParent(): ViewGroup? {
    var view = this
    var fallback: ViewGroup? = null
    do {
        if (view is CoordinatorLayout) {
            return view
        } else if (view is FrameLayout) {
            if (view.id == android.R.id.content) {
                return view
            } else {
                fallback = view
            }
        }
        if (view != null) {
            val parent = view.parent
            view = if (parent is View) parent else null
        }
    } while (view != null)
    return fallback
}