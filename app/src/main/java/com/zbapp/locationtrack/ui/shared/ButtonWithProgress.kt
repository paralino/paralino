package com.zbapp.locationtrack.ui.shared

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.ElementProgressButtonBinding
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.viewBinding

class ButtonWithProgress
    : ConstraintLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttributes(attrs)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        initAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes) {
        initAttributes(attrs)
    }

    private val vb = ElementProgressButtonBinding.inflate(LayoutInflater.from(context), this)

    var itemData: ButtonWithProgressData? = null
        set(value) {
            field = value
            if (value != null)
                applyData(value)
        }

    init {
        vb.buttonHolder.layoutTransition?.setAnimateParentHierarchy(false)
    }

    private fun initAttributes(attrs: AttributeSet?) {
        if(attrs != null) {
            val attributes = context.obtainStyledAttributes(
                attrs, R.styleable.ButtonWithProgress, 0, 0
            )

            val text = attributes.getString(R.styleable.ButtonWithProgress_text) ?: ""
            val icon = attributes.getDrawable(R.styleable.ButtonWithProgress_icon)
            val tint = attributes.getColor(R.styleable.ButtonWithProgress_buttonTint, 0xffffff)
            val contentTint = attributes.getColor(R.styleable.ButtonWithProgress_contentTint, 0x000000)
            itemData = ButtonWithProgressData(text = text, iconDrawable = icon, buttonTint = tint, contentTint = contentTint)
            attributes.recycle()
        } else {
            itemData = ButtonWithProgressData(text = "")
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        vb.btConfirm.setOnClickListener(l)
    }

    private fun applyData(data: ButtonWithProgressData) {
        data.applyTint(vb.btConfirm)
        data.applyTint(vb.buttonTitle, vb.buttonIcon, vb.buttonCheck, vb.buttonProgress)

        vb.btConfirm.isEnabled = data.enabled
        vb.buttonIcon.alpha = if(data.enabled) 1f else 0.4f

        setLoading(false)
        setContent(data.text, data.infoText)

        data.applyIcon(vb.buttonIcon)
        vb.buttonIcon.isVisible = vb.buttonTitle.isVisible == true && itemData?.hasIcon == true
    }

    fun setContent(text: String, infoText: String = "") {
        itemData?.text = text
        itemData?.infoText = infoText

        vb.tvButtonInfo.text = infoText
        vb.buttonTitle.text = text

        vb.tvButtonInfo.isVisible = infoText.isNotBlank()
    }

    fun setText(@StringRes text: Int) {
        itemData?.text = context.getString(text)
        vb.buttonTitle.setText(text)
    }

    fun setLoading(loading: Boolean, withCheck: Boolean = false) {
        val loadingBefore = itemData?.loading ?: false
        itemData?.loading = loading
        setLoadingState(loadingBefore, withCheck)
    }

    private fun setLoadingState(wasLoadingBefore: Boolean, withCheck: Boolean = false) {
        val data = itemData ?: return

        if(data.loading || withCheck) {
            vb.btConfirm.isClickable = false
            vb.buttonIcon.visibility = if(data.hasIcon) View.INVISIBLE else View.GONE
            vb.buttonTitle.isInvisible = true
            vb.buttonProgress.isVisible = data.loading
            vb.buttonCheck.isVisible = !data.loading && withCheck && wasLoadingBefore
            if(!data.loading && withCheck) {
                if(wasLoadingBefore) {
                    vb.buttonCheck.postDelayed(800) { restore() }
                } else {
                    restore()
                }
            }
        } else  {
            if(wasLoadingBefore) {
                vb.buttonHolder.postDelayed(400) { restore() }
            } else {
                restore()
            }
        }
    }

    private fun restore() {
        val data = itemData ?: return
        vb.btConfirm?.isClickable = true
        vb.buttonIcon?.isVisible = data.hasIcon
        vb.buttonTitle?.isVisible = true
        vb.buttonProgress?.isVisible = false
        vb.buttonCheck?.isVisible = false
    }

    fun brieflyShowCheckMark() {
        vb.btConfirm?.isClickable = false
        vb.buttonIcon?.visibility = if(itemData?.hasIcon == true) View.INVISIBLE else View.GONE
        vb.buttonTitle?.isInvisible = true
        vb.buttonProgress?.isVisible = false
        vb.buttonCheck?.isVisible = true
        vb.buttonCheck?.postDelayed(800) {
            restore()
        }
    }

}

data class ButtonWithProgressData constructor(
    var text: String,
    @DrawableRes var iconRes: Int? = null,
    var iconDrawable: Drawable? = null,
    var infoText: String = "",
    var loading: Boolean = false,
    var enabled: Boolean = true,
    @ColorInt val buttonTint: Int? = null,
    @ColorInt val contentTint: Int? = null,
    var id: String = text
): ListItemType {

    val hasIcon: Boolean get() = (iconRes?.takeIf { it != 0 } ?: iconDrawable) != null

    fun applyTint(button: AppCompatButton) {
        buttonTint?.let {
            ViewCompat.setBackgroundTintList(button, ColorStateList.valueOf(it))
        } ?: let { button.backgroundTintList = null }
    }

    fun applyTint(textView: TextView, icon: ImageView, check: ImageView, progress: ProgressBar) {
        contentTint?.let {
            icon.imageTintList = ColorStateList.valueOf(it)
            check.imageTintList = ColorStateList.valueOf(it)
            progress.indeterminateTintList = ColorStateList.valueOf(it)
            textView.setTextColor(it)
        } ?: let {
            val color = icon.context.getColor(R.color.main_button_text_color)
            icon.imageTintList = ColorStateList.valueOf(color)
            check.imageTintList = ColorStateList.valueOf(color)
            progress.indeterminateTintList = ColorStateList.valueOf(color)
            textView.setTextColor(color)
        }
    }

    fun applyIcon(imageView: ImageView) {
        if(!hasIcon) return
        iconRes?.takeIf { it != 0 }?.let {
            imageView.setImageResource(it)
        } ?: imageView.setImageDrawable(iconDrawable)
    }

    override fun getIdentifier(): String {
        return id
    }

}