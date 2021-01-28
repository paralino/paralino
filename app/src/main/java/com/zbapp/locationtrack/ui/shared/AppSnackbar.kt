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
import com.zbapp.locationtrack.utils.extensions.fadeIn
import com.zbapp.locationtrack.utils.extensions.fadeOut
import kotlinx.android.synthetic.main.item_snackbar.view.*

class AppSnackbarView(context: Context, attributeSet: AttributeSet?):
    ConstraintLayout(context, attributeSet), ContentViewCallback {

    var actionClick : (() -> Unit)? = null

    init {
        inflate(context, R.layout.item_snackbar, this)
        clipToPadding = false
        bAction?.setOnClickListener {
            actionClick?.invoke()
        }
    }

    fun dataReady(data: AppSnackbarData) {
        tvText?.text = data.text
        bAction?.isVisible = !data.actionText.isNullOrBlank()
        bAction?.text = data.actionText ?: ""
    }

    override fun animateContentOut(delay: Int, duration: Int) {
        snackbarHolder?.fadeOut(withScaleTo = 0.6f)
    }

    override fun animateContentIn(delay: Int, duration: Int) {
        snackbarHolder?.fadeIn(withScaleFrom = 0.6f)
    }
}

data class AppSnackbarData(
    val text: SpannableString,
    val actionText: String? = null
){
    constructor(text: String, actionText: String?): this(SpannableString(text), actionText)
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
                val duration = if(actionClick == null || data.actionText.isNullOrEmpty()) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_INDEFINITE
                return AppSnackbar(parent, snackbar)
                    .setDuration(duration)
                    .setAnimationMode(ANIMATION_MODE_SLIDE)
            }catch ( e: Exception){ }

            return null
        }

    }

}

fun snackBarSimple(anchorView: View?, content: String,  actionText: String? = null, actionClick : (() -> Unit)? = null) {
    AppSnackbar.make(anchorView, AppSnackbarData(content, actionText), actionClick)?.show()
}

fun snackBarSimple(anchorView: View?, content: SpannableString,  actionText: String? = null, actionClick : (() -> Unit)? = null) {
    AppSnackbar.make(anchorView, AppSnackbarData(content, actionText), actionClick)?.show()
}

fun snackBarError(anchorView: View?, context: Context) {
    AppSnackbar.make(anchorView, AppSnackbarData(context.getString(R.string.error_generic_snacbar), null))?.show()
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