/*
 * Copyright (c) 2020 - Zelimir
 */
package app.paralino.android.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView


abstract class BaseRvItem<T>(context: Context, attributeSet: AttributeSet? = null)
    : ConstraintLayout(context, attributeSet){

    abstract fun applyData(data: T)

    var itemData: T? = null
        set(value) {
            field = value
            if (value != null)
                applyData(value)
        }

    protected fun inflate(@LayoutRes layout: Int) {
        LayoutInflater.from(context).inflate(layout, this, true)
    }

    protected fun inflateFullWidth(@LayoutRes layout: Int) {
        inflate(layout)
        setFullWidth()
    }

    protected fun inflateFullHeight(@LayoutRes layout: Int) {
        inflate(layout)
        setFullHeight()
    }

    protected fun setFullWidth() {
        rootView.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    protected fun setFullHeight() {
        rootView.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}

fun <T, U: BaseRvItem<T>> U.withClickAction (callback: ((U) -> Unit)?): U {
    setOnClickListener { callback?.invoke(this) }
    return this
}

fun <T, U: BaseRvItem<T>> U.withDataOnClickAction (callback: ((T) -> Unit)?): U {
    setOnClickListener { itemData?.let { callback?.invoke(it) } }
    return this
}

fun <T, U: BaseRvItem<T>> U.withLongClickAction (callback: ((U) -> Boolean)?): U {
    setOnLongClickListener { callback?.invoke(this) ?: false }
    return this
}

class DefaultDiffItemCallback: DiffUtil.ItemCallback<Any>(){
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        if(oldItem is ListItemType && newItem is ListItemType)
            return oldItem.getIdentifier() == newItem.getIdentifier()
        return oldItem === newItem
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem == newItem
    }

}

interface ListItemType {
    fun getIdentifier(): String { return toString() }
}

class SimpleRvItem<T>(
    context: Context,
    @LayoutRes layout: Int,
    onInit: (BaseRvItem<T>.() -> Unit) ? = null,
    private val gotData: View.(T) -> Unit
): BaseRvItem<T>(context, null){

    init {
        LayoutInflater.from(context).inflate(layout, this, true)
        onInit?.invoke(this)
    }

    override fun applyData(data: T) {  gotData(this, data)  }
}
