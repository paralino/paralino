package com.zbapp.core.presentation.common

sealed class FormValidation {

    object Email : FormValidation() {
        fun validate(email: String): FormValidationResult {
            val emailValidationPattern = "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"

            return FormValidationResult(
                type = this,
                result = Regex.fromLiteral(emailValidationPattern).matches(email)
            )
        }
    }

    object Password : FormValidation() {
        fun validate(password: String): FormValidationResult {
            return FormValidationResult(this, result = password.length >= 8)
        }
    }

    object PasswordConfirm : FormValidation() {
        fun validate(password: String, confirmPassword: String): FormValidationResult {
            val passwordValidation = Password.validate(password)
            val confirmPasswordValidation = Password.validate(confirmPassword)
            val passwordsMatch = password == confirmPassword

            return FormValidationResult(
                type = this,
                result = passwordValidation.result && confirmPasswordValidation.result && passwordsMatch
            )
        }
    }

}

data class FormValidationResult(
    val type: FormValidation,
    val result: Boolean
)