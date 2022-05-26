package com.zbapp.locationtrack.ui.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController

abstract class BaseFragment(@LayoutRes layoutResId: Int): Fragment(layoutResId) {

    fun <T> LiveData<T>.observe(onChanged: (T) -> Unit){
        observe(viewLifecycleOwner, Observer(onChanged))
    }

}

fun Fragment.navigateDefault(id: Int, bundle: Bundle? = null) {
    findNavController().navigate(id, bundle, BaseNavOptions.default())
}