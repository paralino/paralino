package app.paralino.core.models.user

enum class SignUpStep {
    STARTING,
    CREATING_ACCOUNT,
    LOG_USER_IN,
    GENERATING_KEYS,
    INIT_USER_DATA,
    SAVE_USER,
    ALL_DONE
}

class SignUpStepException constructor(val step: SignUpStep, cause: Throwable) : Exception(cause) {
    override fun toString(): String {
        return super.toString() + "Step: $step"
    }
}