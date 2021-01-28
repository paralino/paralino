/*
 * Copyright (c) 2020 - Zelimir
 */

package com.zbapp.locationtrack.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2


class SimpleRvAdapter: ListAdapter<Any, RecyclerView.ViewHolder>(DefaultDiffItemCallback()){

    private val FALLBACK_VIEW_TYPE = Integer.MAX_VALUE - 1
    private val fallbackItemFactory = RvItemFactory{ object: BaseRvItem<Any>(it){ override fun applyData(data: Any) { } }}
    val factories = mutableListOf<RvItemFactory<Any,*>>()


    fun addFactory(vararg adapter: RvItemFactory<Any,*>): SimpleRvAdapter {
        factories.addAll(adapter.toList())
        return this
    }

    inline fun <reified T, U: BaseRvItem<T>> addFactory(
        noinline view: (parentContext: Context) -> U
    ): SimpleRvAdapter {
        val delegate = RvItemFactory(view, T::class.java)
        factories.add(delegate as RvItemFactory<Any, *>)
        return this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return factories.getOrNull(viewType)?.onCreateViewHolder(parent) ?: fallbackItemFactory.onCreateViewHolder(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = holder.itemView as? BaseRvItem<Any>?
        item?.itemData = currentList[position]
    }

    override fun getItemViewType(position: Int): Int {
        val item = currentList[position]
        factories.forEachIndexed { index, rvItemFactory ->
            if(rvItemFactory.isForViewType(item)){
                return index
            }
        }
        return FALLBACK_VIEW_TYPE
    }

}

open class RvItemFactory<T, U: BaseRvItem<T>> constructor(
    private val factory: (parentContext: Context) -> U,
    private val klass: Class<T>
){

    companion object {
        inline operator fun <reified T, U: BaseRvItem<T>>invoke(
            noinline view: (parentContext: Context) -> U
        ) = RvItemFactory(view, T::class.java)
    }

    open fun isForViewType(items: Any?): Boolean {
        return klass.isAssignableFrom(items?.javaClass ?: return false)
    }

    open fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder{
        return object: RecyclerView.ViewHolder(factory(parent.context)){}
    }

}

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
        rootView.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    protected fun inflateFullHeight(@LayoutRes layout: Int) {
        inflate(layout)
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

inline val RecyclerView.simpleAdapter get() = this.adapter as? SimpleRvAdapter
inline val ViewPager2.simpleAdapter get() = this.adapter as? SimpleRvAdapter
