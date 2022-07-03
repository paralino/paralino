package app.paralino.mobile.ui.login

import android.util.Patterns
import androidx.annotation.StringRes
import app.paralino.mobile.R
import java.lang.Exception

object LoginValidations {

    private fun emailUsername(content: String): LoginValidation.EMAIL? {
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

    private fun server(url: String): LoginValidation.SERVER? {
        if(url.isBlank() || !Patterns.WEB_URL.matcher(url).matches())
            return LoginValidation.SERVER(R.string.form_invalid_server_url)
        return null
    }

    fun login(email: String, password: String): List<LoginValidation> {
        return listOfNotNull(emailUsername(email), password(password))
    }

    fun signup(username: String, email: String, password: String, confirm: String?): List<LoginValidation> {
        return listOfNotNull(username(username), email(email), password(password))
    }

}

sealed class LoginValidation(@StringRes val description: Int) {
    class EMAIL(description: Int) : LoginValidation(description)
    class PASSWORD(description: Int) : LoginValidation(description)
    class CONFIRM_PASSWORD(description: Int) : LoginValidation(description)
    class USERNAME(description: Int) : LoginValidation(description)
    class SERVER(description: Int) : LoginValidation(description)
}

class ValidationException(val validation: LoginValidation): Exception()