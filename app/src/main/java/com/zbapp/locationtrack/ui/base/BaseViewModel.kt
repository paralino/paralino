package com.zbapp.locationtrack.ui.base

import androidx.lifecycle.AndroidViewModel
import com.zbapp.locationtrack.AppClass

val AndroidViewModel.myApp: AppClass get() = getApplication() as AppClass