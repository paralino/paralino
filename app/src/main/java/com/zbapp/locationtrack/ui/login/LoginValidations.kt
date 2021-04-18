package com.zbapp.locationtrack.ui.login

import android.util.Patterns
import androidx.annotation.StringRes
import com.zbapp.locationtrack.R
import java.lang.Exception

object LoginValidations {

    fun emailUsername(content: String): LoginValidation.EMAIL? {
        if(content.isBlank())
            return LoginValidation.EMAIL(R.string.form_invalid_username_email_description)
        else if (!content.contains("@"))
            return username(content)?.let { LoginValidation.EMAIL(it.description) }
        return email(content)
    }

    fun email(email: String): LoginValidation.EMAIL? {
        if(email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return LoginValidation.EMAIL(R.string.form_invalid_email_description)
        return null
    }

    private fun username(username: String): LoginValidation.USERNAME? {
        if(username.length < 6)
            return LoginValidation.USERNAME(R.string.form_invalid_username_length_description)
        else if(!Regex("^[a-zA-Z0-9\\-_]+\$").matches(username))
            return LoginValidation.USERNAME(R.string.form_invalid_username_description)
        return null
    }

    private fun password(password: String): LoginValidation.PASSWORD? {
        if(password.length < 8)
            return LoginValidation.PASSWORD(R.string.form_invalid_password_length_description)
        return null
    }

    private fun passwordConfirm(password: String, confirm: String): LoginValidation.CONFIRM_PASSWORD? {
        if(password.length < 8)
            return LoginValidation.CONFIRM_PASSWORD(R.string.form_invalid_password_length_description)
        else if(password != confirm)
            return LoginValidation.CONFIRM_PASSWORD(R.string.form_invalid_password_match_description)
        return null
    }

    fun login(email: String, password: String): List<LoginValidation> {
        return listOfNotNull(emailUsername(email), password(password))
    }

    fun signup(username: String, email: String, password: String, confirm: String): List<LoginValidation> {
        return listOfNotNull(username(username), email(email), password(password))
    }

}

sealed class LoginValidation(@StringRes val description: Int) {
    class EMAIL(description: Int) : LoginValidation(description)
    class PASSWORD(description: Int) : LoginValidation(description)
    class CONFIRM_PASSWORD(description: Int) : LoginValidation(description)
    class USERNAME(description: Int) : LoginValidation(description)
}

class ValidationException(val validation: LoginValidation): Exception()