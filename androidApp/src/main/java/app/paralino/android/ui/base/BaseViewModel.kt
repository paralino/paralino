package app.paralino.android.ui.base

import androidx.lifecycle.AndroidViewModel
import app.paralino.android.AppClass

val AndroidViewModel.myApp: AppClass get() = getApplication() as AppClass