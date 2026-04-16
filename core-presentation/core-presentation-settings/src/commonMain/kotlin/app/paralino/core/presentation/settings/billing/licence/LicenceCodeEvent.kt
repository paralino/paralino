package app.paralino.core.presentation.settings.billing.licence

sealed class LicenceCodeEvent {
    object OnSuccess : LicenceCodeEvent()
    object OnNavigateBack : LicenceCodeEvent()
}

sealed class LicenceCodeUiEvent {
    data class OnCodeChange(val code: String) : LicenceCodeUiEvent()
    object OnSubmit : LicenceCodeUiEvent()
    object OnBackClick : LicenceCodeUiEvent()
}
