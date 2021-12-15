package com.zbapp.locationtrack.ui.base

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainActivity

abstract class BaseFragment(@LayoutRes layoutResId: Int): Fragment(layoutResId) {

    protected val mainActivity: MainActivity? get() = activity as? MainActivity?

    fun <T> LiveData<T>.observe(onChanged: (T) -> Unit){
        observe(viewLifecycleOwner, Observer(onChanged))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar)?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

}

fun Fragment.navigateDefault(id: Int, bundle: Bundle? = null) {
    findNavController().navigate(id, bundle, BaseNavOptions.default())
}