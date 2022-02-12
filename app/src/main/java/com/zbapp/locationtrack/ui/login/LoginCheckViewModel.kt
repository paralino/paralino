package com.zbapp.locationtrack.ui.login

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.zbapp.core.models.Payload
import com.zbapp.core.repo.user.LoginManager
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.base.myApp
import com.zbapp.locationtrack.ui.shared.AppSnackbarData
import com.zbapp.locationtrack.ui.shared.SingleLiveEvent
import com.zbapp.locationtrack.ui.shared.errorStringRes
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@SuppressLint("StaticFieldLeak")
class LoginCheckViewModel(
    app: Application,
    private val loginManager: LoginManager
): AndroidViewModel(app) {

    private val _errorState = SingleLiveEvent<AppSnackbarData>()
    val errorState = _errorState

    fun checkAndSync(): LiveData<Payload<Unit>> {
        return loginManager.syncUserAndData()
            .map<Unit, Payload<Unit>> { Payload.Success(Unit) }
            .onStart { emit(Payload.Loading) }
            .catch {
                _errorState.value = AppSnackbarData(myApp.getString(it.errorStringRes() ?: R.string.error_generic_snacbar), null)
                emit(Payload.Error(it))
            }
            .asLiveData()

    }

}