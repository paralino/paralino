package com.zbapp.locationtrack.ui.login

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.*
import com.zbapp.core.repo.user.LoginManager
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.shared.AppSnackbarData
import com.zbapp.locationtrack.ui.shared.SingleLiveEvent
import kotlinx.coroutines.flow.*
import kotlin.math.log

@SuppressLint("StaticFieldLeak")
class LoginViewModel(
    private val context: Context,
    private val loginManager: LoginManager
) : ViewModel() {

    private val _loginState = MutableLiveData(LoginState.LOGIN)
    val loginState: LiveData<LoginState> = _loginState.distinctUntilChanged()
    
    private val _loginStateValidation = SingleLiveEvent<LoginValidation?>()
    val loginStateValidation = _loginStateValidation

    private val _errorState = SingleLiveEvent<AppSnackbarData>()
    val errorState = _errorState

    private val _randomUsername = SingleLiveEvent<String>()
    val randomUsername = _randomUsername

    fun loginOrSignUp(username: String, email: String, password: String, confirmPassword: String = "") {
        when (loginState.value) {
            LoginState.LOGIN -> {
                val validation = LoginValidations.login(email, password).firstOrNull()
                updateValidationState(validation)
                if (validation == null)
                    loginManager.login(email, password)
                else flow { }
            }
            LoginState.SIGNUP -> {
                val validation =
                    LoginValidations.signup(username, email, password, confirmPassword).firstOrNull()
                updateValidationState(validation)
                if (validation == null)
                    loginManager.signUp(username, email, password)
                else flow { }
            }
            LoginState.FORGOT_PASSWORD -> {
                val validation = LoginValidations.email(email)
                updateValidationState(validation)
                if (validation == null)
                    flow { }
                else flow { }
            }
            null -> flow { Unit }
        }
            .map { true }
            .catch {
                _errorState.value = AppSnackbarData(context.getString(R.string.error_generic_snacbar), null)
                emit(false)
            }
            .launchIn(viewModelScope)
    }

    private fun updateValidationState(validation: LoginValidation?) {
        _loginStateValidation.value = validation
        if(validation != null)
            _errorState.value = AppSnackbarData(context.getString(validation.description), null)
    }

    fun generateUsername() {
        loginManager.generateUsername()
            .onEach { _randomUsername.value = it }
            .catch {
                _errorState.value = AppSnackbarData(context.getString(R.string.error_generic_snacbar), null)
            }
            .launchIn(viewModelScope)
    }

    fun toggleLoginSignup() {
        updateValidationState(null)
        when (loginState.value) {
            LoginState.LOGIN -> _loginState.postValue(LoginState.SIGNUP)
            LoginState.SIGNUP -> _loginState.postValue(LoginState.LOGIN)
            LoginState.FORGOT_PASSWORD -> _loginState.postValue(LoginState.LOGIN)
            else -> {}
        }
    }

    fun toggleForgotPassword() {
        updateValidationState(null)
        when (loginState.value) {
            LoginState.LOGIN -> _loginState.postValue(LoginState.FORGOT_PASSWORD)
            LoginState.FORGOT_PASSWORD -> _loginState.postValue(LoginState.LOGIN)
            else -> {}
        }
    }


}

enum class LoginState {
    LOGIN, SIGNUP, FORGOT_PASSWORD
}