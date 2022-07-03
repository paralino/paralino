package app.paralino.mobile.ui.base

import androidx.lifecycle.AndroidViewModel
import app.paralino.mobile.AppClass

val AndroidViewModel.myApp: AppClass get() = getApplication() as AppClass