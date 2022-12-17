package app.paralino.core.presentation.settings.editprofile

import app.paralino.core.config
import app.paralino.core.domain.user.UpdateUserDataUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.user.UserMeData
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditProfileViewModel constructor(
    private val userRepository: UserRepository,
    private val updateUserData: UpdateUserDataUseCase,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<EditProfileScreenState>(
        EditProfileScreenState.fromUser(userRepository.cachedUser?.data)
            .copy(deviceName = config.deviceName)
    )
    val uiState: Flow<EditProfileScreenState> = _uiState

    private val _eventChannel = Channel<EditProfileScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    fun proceed() {
        val image = _uiState.value.image
        val name = _uiState.value.name
        val deviceName = _uiState.value.deviceName

        val nameValidation = FormValidation.TextNotEmpty.validate(name)

        _uiState.update {
            it.copy(hasNameError = !nameValidation.result)
        }

        if (!nameValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(EditProfileScreenEvent.ValidationError(nameValidation))
            }
        } else {
            val userData = UserMeData(userImage = image, userName = name)
            updateUserData.updateUserData(userData, deviceName)
                .mapToPayload { it }
                .onEach { result ->
                    when (result) {
                        Payload.Loading -> {
                            _uiState.update { it.copy(actionLoading = true) }
                        }
                        is Payload.Error -> {
                            _uiState.update { it.copy(actionLoading = null) }
                            _eventChannel.send(EditProfileScreenEvent.Error(result.exception))
                        }
                        is Payload.Success -> {
                            _uiState.update { it.copy(actionLoading = false) }
                            delay(400)
                            _eventChannel.send(EditProfileScreenEvent.ProfileUpdated)
                        }
                    }
                }
                .launchIn(coroutineScope)
        }
    }

    fun onImageChange(image: ImageModel) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update { it.copy(image = image) }
    }

    fun onNameChange(name: String) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update { it.copy(name = name, hasNameError = false) }
    }

    fun onDeviceNameChange(name: String) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update { it.copy(deviceName = name, hasNameError = false) }
    }

}