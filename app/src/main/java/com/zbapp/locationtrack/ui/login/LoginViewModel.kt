package com.zbapp.locationtrack.ui.login

import android.app.Application
import android.text.SpannableString
import androidx.lifecycle.*
import com.google.android.material.snackbar.Snackbar
import com.zbapp.core.models.Payload
import com.zbapp.core.repo.user.LoginManager
import com.zbapp.core.repo.user.SignUpStep
import com.zbapp.core.repo.user.SignUpStepException
import com.zbapp.core.utils.exceptions.ConflictException
import com.zbapp.core.utils.exceptions.UnauthorizedException
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.base.myApp
import com.zbapp.locationtrack.ui.shared.AppSnackbarData
import com.zbapp.locationtrack.ui.shared.SingleLiveEvent
import com.zbapp.locationtrack.ui.shared.errorString
import com.zbapp.locationtrack.utils.extensions.attributedBold
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class LoginViewModel(
    app: Application,
    private val loginManager: LoginManager
) : AndroidViewModel(app) {

    private val _loginScreenState = MutableLiveData(LoginState.LOGIN)
    val loginScreenState: LiveData<LoginState> = _loginScreenState.distinctUntilChanged()

    private val _loginInfoState = MutableLiveData("")
    val loginInfoState: LiveData<String> = _loginInfoState.distinctUntilChanged()

    private val _loginStateValidation = SingleLiveEvent<List<LoginValidation>>()
    val loginStateValidation = _loginStateValidation

    private val _errorState = SingleLiveEvent<AppSnackbarData>()
    val errorState = _errorState


    fun loginOrSignUp(
        userName: String,
        email: String,
        password: String,
        confirmPassword: String = ""
    ): LiveData<Payload<SignUpStep>> {

        val loginFlow = when (loginScreenState.value) {
            LoginState.LOGIN -> {
                val validation = LoginValidations.login(email, password)
                updateValidationState(validation)
                if (validation.isEmpty())
                    loginManager.login(email, password)
                else flow { throw ValidationException(validation.first()) }
            }
            LoginState.SIGNUP -> {
                val validation =
                    LoginValidations.signup(userName, email, password, null)
                updateValidationState(validation)
                if (validation.isEmpty())
                    loginManager.signUp(userName, email, password)
                else flow { throw ValidationException(validation.first()) }
            }
            LoginState.FORGOT_PASSWORD -> {
                val validation = LoginValidations.email(email).let { listOfNotNull(it) }
                updateValidationState(validation)
                if (validation.isEmpty())
                    flow {
                        // not implemented yet
                        emit(SignUpStep.ALL_DONE)
                    }
                else flow { throw ValidationException(validation.first()) }
            }
            null -> flow { emit(SignUpStep.ALL_DONE) }
        }

        return loginFlow
            .map<SignUpStep, Payload<SignUpStep>> {
                _loginInfoState.value = getSignUpStepInfo(it)
                Payload.Success(it)
            }
            .onStart { emit(Payload.Loading) }
            .catch {
                val error = when (it) {
                    is ValidationException -> myApp.attributedBold(it.validation.description)
                    is SignUpStepException -> getSignUpStepError(it)
                    else -> myApp.errorString(it)
                }

                _loginInfoState.value = ""
                _errorState.value =
                    AppSnackbarData(error?.toString() ?: myApp.getString(R.string.error_generic_snacbar), null, Snackbar.LENGTH_LONG)

                emit(Payload.Error(it))
            }
            .asLiveData()
    }

    private fun updateValidationState(validation: List<LoginValidation>?) {
        _loginStateValidation.value = validation ?: emptyList()
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

    private fun getSignUpStepInfo(step: SignUpStep): String {
        val infoRes = when (step) {
            SignUpStep.STARTING -> {
                when (loginScreenState.value) {
                    LoginState.LOGIN -> R.string.login_step_logIn
                    LoginState.SIGNUP -> R.string.login_step_signUp
                    else -> null
                }
            }
            SignUpStep.GENERATING_KEYS -> R.string.login_step_encryption
            SignUpStep.INIT_USER_DATA -> R.string.login_step_user_setup
            SignUpStep.SAVE_USER -> R.string.login_step_user_setup
            SignUpStep.ALL_DONE -> R.string.login_step_allDone
        }
        return infoRes?.let { myApp.getString(it) } ?: ""
    }

    private fun getSignUpStepError(error: SignUpStepException): SpannableString? {
        val defaultRes: SpannableString? = when (error.cause) {
            is ConflictException -> myApp.attributedBold(R.string.form_invalid_username_conflict_description)
            is UnauthorizedException -> myApp.attributedBold(R.string.form_invalid_credentials_description)
            else -> error.cause?.let { myApp.errorString(it) }
        }

        if (defaultRes != null)
            return defaultRes

        val errorRes: Int? = when (error.step) {
            SignUpStep.STARTING -> {
                when (loginScreenState.value) {
                    LoginState.LOGIN -> R.string.login_step_error_logIn
                    LoginState.SIGNUP -> R.string.login_step_error_signUp
                    else -> null
                }
            }
            SignUpStep.GENERATING_KEYS -> {
                when (loginScreenState.value) {
                    LoginState.LOGIN -> R.string.login_step_error_encryption_logIn
                    LoginState.SIGNUP -> R.string.login_step_error_encryption_signUp
                    else -> null
                }
            }
            // TODO ofer user an option to generate keys again
            SignUpStep.INIT_USER_DATA -> {
                when (loginScreenState.value) {
                    LoginState.LOGIN -> R.string.login_step_error_user_setup_logIn
                    LoginState.SIGNUP -> R.string.login_step_error_user_setup_signUp
                    else -> null
                }
            }
            SignUpStep.SAVE_USER -> {
                when (loginScreenState.value) {
                    LoginState.LOGIN -> R.string.login_step_error_user_save_logIn
                    LoginState.SIGNUP -> R.string.login_step_error_user_save_signUp
                    else -> null
                }
            }
            SignUpStep.ALL_DONE -> null
        }

        return errorRes?.let { myApp.attributedBold(it) }
    }


}

enum class LoginState {
    LOGIN, SIGNUP, FORGOT_PASSWORD
}