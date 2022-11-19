package app.paralino.android.ui.rawdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.crypto.RawCryptoPresentation
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType

class RawDataPresentationViewModel(
    rawCryptoPresentation: RawCryptoPresentation,
    rawPresentationDataType: RawPresentationDataType
) : ViewModel() {

    private val coreVm =
        RawDataPresentationViewModel(rawCryptoPresentation, rawPresentationDataType, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

}