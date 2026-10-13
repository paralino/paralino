package app.paralino.core.presentation.settings.status

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.LocationProviderType
import app.paralino.core.provider.ConnectionStatus
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.utils.ConditionErrorType

data class AppStatusScreenState(
    val requirements: List<AppStatusRequirementState> = emptyList(),
    val connection: AppStatusConnectionState? = null,
    val apiTest: AppStatusApiTestState = AppStatusApiTestState.Idle,
    val locationMode: AppStatusLocationModeState? = null,
    val sharing: AppStatusSharingState = AppStatusSharingState(),
    val currentLocation: AppStatusCurrentLocationState? = null
)

data class AppStatusRequirementState(
    val type: ConditionErrorType,
    val isResolved: Boolean
)

data class AppStatusConnectionState(
    val status: ConnectionStatus,
    val networkType: NetworkType?,
    val signalStrength: NetworkSignalStrength?
)

sealed class AppStatusApiTestState {
    data object Idle : AppStatusApiTestState()
    data object Loading : AppStatusApiTestState()
    data object Success : AppStatusApiTestState()
    data class Failed(val failedTags: List<String>) : AppStatusApiTestState()
}

data class AppStatusLocationModeState(
    val current: String,
    val requested: String?,
    val isThrottled: Boolean,
    val allowances: List<AppStatusAllowanceState>
)

data class AppStatusAllowanceState(
    val name: String,
    val windowMinutes: Long,
    val time: String?,
    val slots: List<Float>?,
    val isUsedUp: Boolean
)

data class AppStatusSharingState(
    val groups: List<AppStatusSharingTargetState> = emptyList(),
    val personalSpaceImage: ImageModel? = null,
    val isSharingToPersonalSpace: Boolean = false
)

data class AppStatusSharingTargetState(
    val id: String,
    val name: String,
    val image: ImageModel
)

data class AppStatusCurrentLocationState(
    val coordinates: String,
    val time: String?,
    val accuracy: String,
    val isAccuracyLow: Boolean,
    val speed: String?,
    val elevation: String?,
    val provider: LocationProviderType?,
    val isFiltered: Boolean
)

sealed class AppStatusScreenUiEvent {
    data object TriggerAppUpdate : AppStatusScreenUiEvent()
    data object TestApiConnection : AppStatusScreenUiEvent()
    data object ClearApiTestResult : AppStatusScreenUiEvent()
}

val LocationPerformanceType.displayName: String
    get() = when (this) {
        LocationPerformanceType.VERY_HIGH -> "Realtime boost"
        else -> name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

val LocationPerformanceType.displayNameWithIndex: String
    get() {
        return displayName.plus(" (level ${this.ordinal + 1})")
    }
