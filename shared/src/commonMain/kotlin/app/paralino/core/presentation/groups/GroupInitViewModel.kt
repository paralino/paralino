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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupInitViewModel(
    private val connectionStatus: IConnectionStatusProvider,
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
            personalCodesRepo.getAndRefreshMyPersonalCodes()
        ) { user, network, codes ->

            val errors = buildList {
                if (!network.isOnline)
                    add(ConditionErrorType.NETWORK)

                user.data?.needsEmailVerification?.takeIf { it }?.let {
                    add(ConditionErrorType.EMAIL_VERIFICATION(user.data?.email ?: ""))
                }
            }

            val code = codes.firstOrNull()
            val personalCode = code?.data?.data?.let {
                PersonalCodeScreenCodeData(
                    id = code.id,
                    code = it.formattedCode,
                    canDelete = false,
                    validityUTC = code.validityUTC
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