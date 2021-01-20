package com.zbapp.locationtrack.ui.base

import androidx.navigation.NavOptions
import com.zbapp.locationtrack.R

object BaseNavOptions {
    fun defaultNavOptionsBuilder(): NavOptions.Builder {
        return NavOptions.Builder()
            .setEnterAnim(R.anim.nav_default_enter_anim)
            .setExitAnim(R.anim.nav_default_exit_anim)
            .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
            .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
    }

    fun default(): NavOptions = defaultNavOptionsBuilder().build()
}