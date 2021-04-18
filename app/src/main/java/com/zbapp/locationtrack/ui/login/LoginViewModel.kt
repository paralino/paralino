package com.zbapp.locationtrack.ui.login

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.*
import com.zbapp.core.models.Payload
import com.zbapp.core.repo.user.LoginManager
import com.zbapp.core.utils.ConflictException
import com.zbapp.core.utils.NotFoundException
import com.zbapp.core.utils.UnauthorizedException
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.shared.AppSnackbarData
import com.zbapp.locationtrack.ui.shared.SingleLiveEvent
import com.zbapp.locationtrack.ui.shared.errorStringRes
import kotlinx.coroutines.flow.*

@SuppressLint("StaticFieldLeak")
class LoginViewModel(
    private val context: Context,
    private val loginManager: LoginManager
) : ViewModel() {

    private val _loginScreenState = MutableLiveData(LoginState.LOGIN)
    val loginScreenState: LiveData<LoginState> = _loginScreenState.distinctUntilChanged()
    
    private val _loginStateValidation = SingleLiveEvent<LoginValidation?>()
    val loginStateValidation = _loginStateValidation

    private val _errorState = SingleLiveEvent<AppSnackbarData>()
    val errorState = _errorState

    private val _randomUsername = SingleLiveEvent<String>()
    val randomUsername = _randomUsername

    fun loginOrSignUp(username: String, email: String, password: String, confirmPassword: String = ""): LiveData<Payload<Unit>> {
        return when (loginScreenState.value) {
            LoginState.LOGIN -> {
                val validation = LoginValidations.login(email, password).firstOrNull()
                updateValidationState(validation)
                if (validation == null)
                    loginManager.login(email, password)
                else flow { throw ValidationException(validation) }
            }
            LoginState.SIGNUP -> {
                val validation =
                    LoginValidations.signup(username, email, password, confirmPassword).firstOrNull()
                updateValidationState(validation)
                if (validation == null)
                    loginManager.signUp(username, email, password)
                else flow { throw ValidationException(validation) }
            }
            LoginState.FORGOT_PASSWORD -> {
                val validation = LoginValidations.email(email)
                updateValidationState(validation)
                if (validation == null)
                    flow {
                        // not implemented yet
                        emit(Unit)
                    }
                else flow { throw ValidationException(validation) }
            }
            null -> flow { emit(Unit) }
        }
            .map<Unit, Payload<Unit>> { Payload.Success(Unit) }
            .onStart { emit(Payload.Loading) }
            .catch {
                val error = when(it) {
                    is ValidationException -> it.validation.description
                    is NotFoundException -> R.string.error_user_not_found
                    is ConflictException -> R.string.form_invalid_username_conflict_description
                    is UnauthorizedException -> R.string.form_invalid_credentials_description
                    else -> it.errorStringRes() ?: R.string.error_generic_snacbar
                }

                _errorState.value = AppSnackbarData(context.getString(error), null)

                emit(Payload.Error(it))
            }
            .asLiveData()
    }

    private fun updateValidationState(validation: LoginValidation?) {
        _loginStateValidation.value = validation
    }

    fun generateUsername() {
        loginManager.generateUsername()
            .onEach { _randomUsername.value = it }
            .catch {
                _errorState.value = AppSnackbarData(context.getString(R.string.error_generic_snacbar), null)
            }
            .launchIn(viewModelScope)
    }

    fun isUserVerified(): Boolean {
        return loginManager.isVerified()
    }

    fun toggleLoginSignup() {
        updateValidationState(null)
        when (loginScreenState.value) {
            LoginState.LOGIN -> _loginScreenState.postValue(LoginState.SIGNUP)
            LoginState.SIGNUP -> _loginScreenState.postValue(LoginState.LOGIN)
            LoginState.FORGOT_PASSWORD -> _loginScreenState.postValue(LoginState.LOGIN)
            else -> {}
        }
    }

    fun toggleForgotPassword() {
        updateValidationState(null)
        when (loginScreenState.value) {
            LoginState.LOGIN -> _loginScreenState.postValue(LoginState.FORGOT_PASSWORD)
            LoginState.FORGOT_PASSWORD -> _loginScreenState.postValue(LoginState.LOGIN)
            else -> {}
        }
    }


}

enum class LoginState {
    LOGIN, SIGNUP, FORGOT_PASSWORD
}