package app.paralino.core.presentation.settings.subscriptions

import app.paralino.core.data.db.AppPrefs
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class PlanInfoViewModel(
    private val userRepo: UserRepository,
    private val appPrefs: AppPrefs
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        userRepo.userPlans.value.highestValidPlanConfig
    )
    val uiState: StateFlow<PlanConfig> = _uiState

    init {
        userRepo.userPlans
            .onEach { plans ->
                _uiState.update { plans.highestValidPlanConfig }
            }
            .launchIn(coroutineScope)
    }

    fun confirmFreePlan() {
        appPrefs.didStartedGroupOnboarding = true
    }
}