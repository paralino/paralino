package app.paralino.core.presentation.settings.billing.licence

import app.paralino.core.presentation.common.MainAlertDialog

data class LicenceCodeState(
    val code: String = "",
    val isLoading: Boolean = false,
    val alertDialog: MainAlertDialog? = null,
    val isErrorStep: Boolean = false,
    val stepText: String? = null
)

