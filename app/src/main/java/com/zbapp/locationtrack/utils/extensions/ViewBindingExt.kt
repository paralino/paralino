package com.zbapp.locationtrack.utils.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.viewbinding.ViewBinding
import java.lang.IllegalArgumentException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// Source:
// https://gist.github.com/Zhuinden/ea3189198938bd16c03db628e084a4fa#file-fragmentviewbindingdelegate-kt
// https://medium.com/@Zhuinden/simple-one-liner-viewbinding-in-fragments-and-activities-with-kotlin-961430c6c07c


//fragment
class FragmentViewBindingDelegate<T : ViewBinding>(
    val fragment: Fragment,
    val viewBindingFactory: (View) -> T
) : ReadOnlyProperty<Fragment, T> {
    private var binding: T? = null

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            val viewLifecycleOwnerLiveDataObserver =
                Observer<LifecycleOwner?> {
                    val viewLifecycleOwner = it ?: return@Observer

                    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            binding = null
                        }
                    })
                }

            override fun onCreate(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.observeForever(viewLifecycleOwnerLiveDataObserver)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.removeObserver(viewLifecycleOwnerLiveDataObserver)
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        val binding = binding
        if (binding != null) {
            return binding
        }

        val lifecycle = fragment.viewLifecycleOwner.lifecycle
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            throw IllegalStateException("Should not attempt to get bindings when Fragment views are destroyed.")
        }

        return viewBindingFactory(thisRef.requireView()).also { this.binding = it }
    }
}

fun <T : ViewBinding> Fragment.viewBinding(viewBindingFactory: (View) -> T) =
    FragmentViewBindingDelegate(this, viewBindingFactory)

// activity
inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T) =
    lazy(LazyThreadSafetyMode.NONE) {
        bindingInflater.invoke(layoutInflater)
    }

// custom views
class CustomViewBindingDelegate<out T : ViewBinding>(
    val view: View,
    val viewBindingFactory: (LayoutInflater, ViewGroup, Boolean) -> T
) : ReadOnlyProperty<View, T> {

    private var binding: T? = null

    init {
        view.doOnAttach {
            val lifecycleOwner = checkNotNull(view.findViewTreeLifecycleOwner()) {
                "View: $view does not appear to have a lifecycleOwner"
            }
            lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    binding = null
                }
            })
        }
    }

    override fun getValue(thisRef: View, property: KProperty<*>): T {
        val binding = binding
        if (binding != null) {
            return binding
        }

        return viewBindingFactory.invoke(LayoutInflater.from(thisRef.context), thisRef as ViewGroup, true)
            .also { this@CustomViewBindingDelegate.binding = it }
    }
}

fun <T : ViewBinding> View.viewBinding(viewBindingFactory: (LayoutInflater, ViewGroup, Boolean) -> T) =
    CustomViewBindingDelegate(this, viewBindingFactory)

fun <T : ViewBinding> View.viewBinding(viewBindingFactory: (LayoutInflater, ViewGroup) -> T) =
    CustomViewBindingDelegate(this) { i, v, _ -> viewBindingFactory(i, v) }

