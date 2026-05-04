package app.paralino.core.models.settings

import app.paralino.core.domain.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

sealed class FormValidation {

    object Email : FormValidation() {
        fun validate(email: String): FormValidationResult {
            val emailValidationPattern =
                "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                        "\\@" +
                        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                        "(" +
                        "\\." +
                        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                        ")+"

            return FormValidationResult(
                type = this,
                result = Regex(emailValidationPattern).matches(email)
            )
        }
    }

    object Password : FormValidation() {

        data class PasswordValidationResult(
            val minPasswordLength: Boolean,
            val passwordStrength: PasswordStrength
        ) : IFormValidationResult {
            override val type: FormValidation = Password
            override val result: Boolean =
                passwordStrength.isOK && minPasswordLength
        }

        fun validate(password: String): PasswordValidationResult {
            return PasswordValidationResult(
                minPasswordLength = password.length >= PasswordStrength.minPasswordLength,
                passwordStrength = evaluatePasswordStrength(
                    password = password,
                    dictionary = emptyList()
                ) ?: PasswordStrength.VERY_WEAK
            )
        }

        fun validateWithDictionary(
            password: String,
            dictionary: List<String>
        ): PasswordValidationResult {
            return PasswordValidationResult(
                minPasswordLength = password.length >= PasswordStrength.minPasswordLength,
                passwordStrength = evaluatePasswordStrength(
                    password = password,
                    dictionary = dictionary
                ) ?: PasswordStrength.VERY_WEAK
            )
        }

        @OptIn(ExperimentalResourceApi::class)
        suspend fun defaultPasswordDictionary(): List<String> {
            return try {
                Res.readBytes("files/password_list_top_10000.txt")
                    .decodeToString()
                    .lines()
            } catch (e: Exception) {
                emptyList()
            }
        }

        enum class PasswordStrength {
            VERY_WEAK, WEAK, MODERATE, STRONG, VERY_STRONG;

            companion object {
                const val minPasswordLength = 8
                val minPasswordQuality = PasswordStrength.WEAK
            }

            val isOK: Boolean get() = this >= minPasswordQuality
        }

        private fun evaluatePasswordStrength(
            password: String,
            dictionary: List<String>
        ): Password.PasswordStrength? {

            if (password.isBlank()) {
                return null
            }

            if (password.length < PasswordStrength.minPasswordLength) {
                return Password.PasswordStrength.VERY_WEAK
            }

            val characterVariety = password.groupBy { it }.size

            if (characterVariety < 3) {
                return Password.PasswordStrength.VERY_WEAK
            }

            var score = 0
            var typeVariety = 0

            // Check password length
            if (characterVariety > 4) score++
            if (password.length >= 12) score++

            // Check for character variety
            if (password.any { it.isDigit() }) {
                score++
                typeVariety++
            }
            if (password.any { it.isLowerCase() }) {
                score++
                typeVariety++
            }
            if (password.any { it.isUpperCase() }) {
                score++
                typeVariety++
            }
            if (password.any { !it.isLetterOrDigit() }) {
                score++
                typeVariety++
            }

            if (typeVariety <= 1) {
                return Password.PasswordStrength.VERY_WEAK
            }

            if (dictionary.contains(password)) {
                return Password.PasswordStrength.VERY_WEAK
            }

            return when {
                score <= 0 -> null
                score == 1 -> Password.PasswordStrength.VERY_WEAK
                score <= 3 -> Password.PasswordStrength.WEAK
                score == 4 -> Password.PasswordStrength.MODERATE
                score == 5 -> Password.PasswordStrength.STRONG
                else -> Password.PasswordStrength.VERY_STRONG
            }
        }
    }

    object PasswordConfirm : FormValidation() {
        data class PasswordConfirmValidationResult(
            val minPasswordLength: Boolean,
            val passwordStrength: Password.PasswordStrength,
            val passwordsMatch: Boolean
        ) : IFormValidationResult {
            override val type: FormValidation = PasswordConfirm
            override val result: Boolean =
                passwordStrength.isOK && minPasswordLength && passwordsMatch
        }

        fun validate(password: String, confirmPassword: String): PasswordConfirmValidationResult {
            val passwordValidation = Password.validate(password)
//            val confirmPasswordValidation = Password.validate(confirmPassword)
            val passwordsMatch = password == confirmPassword

            return PasswordConfirmValidationResult(
                minPasswordLength = passwordValidation.minPasswordLength,
                passwordStrength = passwordValidation.passwordStrength,
                passwordsMatch = passwordsMatch && password.isNotBlank()
            )
        }
    }

    object TextNotEmpty : FormValidation() {
        fun validate(text: String): FormValidationResult {
            return FormValidationResult(this, result = text.isNotBlank())
        }
    }
}

data class FormValidationResult(
    override val type: FormValidation,
    override val result: Boolean
) : IFormValidationResult

interface IFormValidationResult {
    val type: FormValidation
    val result: Boolean
}