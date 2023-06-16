package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ILocationStatusProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupInitViewModel(
    private val connectionStatus: IConnectionStatusProvider,
    val locationStatusProvider: ILocationStatusProvider,
    private val userRepo: UserRepository,
    private val personalCodesRepo: PersonalCodesRepository
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(GroupInitScreenState())
    val uiState: Flow<GroupInitScreenState> = _uiState

    private val _eventChannel = Channel<GroupInitScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        combine(
            userRepo.localUser,
            connectionStatus.status,
            locationStatusProvider.status,
            personalCodesRepo.getAndRefreshMyPersonalCodes()
        ) { user, network, locationStatus, codes ->

            val errors = buildList {
                if (!network.isOnline)
                    add(ConditionErrorType.NETWORK)

                if(!locationStatus.isNotificationPermissionAllowed)
                    add(ConditionErrorType.PERMISSION_NOTIFICATIONS)

                user.data?.needsEmailVerification?.takeIf { it }?.let {
                    add(ConditionErrorType.EMAIL_VERIFICATION(user.data?.email ?: ""))
                }
            }

            val code = codes.firstOrNull { it.isStillValid() }
            val personalCode = code?.let {
                PersonalCodeScreenCodeData(
                    id = code.id,
                    code = it.formattedCode,
                    userImage = it.codeData.data?.ownerImage ?: ImageModel.emoji(Emojis.robot),
                    userName = it.codeData.data?.ownerName ?: "---",
                    canDelete = false,
                    isValid = true,
                )
            }

            GroupInitScreenState(
                userImage = user.data?.data?.userImage ?: ImageModel.emoji(Emojis.robot),
                personalCode = personalCode,
                errors = errors
            )
        }
            .onEach { result ->
                _uiState.update { result }
            }
            .catch {
                _eventChannel.send(GroupInitScreenEvent.Error(it))
            }
            .launchIn(coroutineScope)
    }

    fun sendEmailVerification() {
        coroutineScope.launch {
            try {
                userRepo.initEmailVerification()
                _eventChannel.send(GroupInitScreenEvent.EmailVerificationSent)
            } catch (e: Exception) {
                _eventChannel.send(GroupInitScreenEvent.Error(e))
            }
        }
    }

}