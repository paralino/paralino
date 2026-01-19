package app.paralino.android.ui.places

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.google.PlaceCreateMapComposable
import app.paralino.core.MR
import app.paralino.core.models.data
import app.paralino.core.presentation.places.create.CreatePlaceScreenUiEvent
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.EmojiBottomSheetPicker
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.components.rememberEmojiBottomSheetPickerState
import app.paralino.core.ui.places.CreatePlaceScreenCore
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CreatePlaceScreen(
    appState: MainAppState,
    groupId: String,
    placeId: String?,
    lat: Double? = null,
    lng: Double? = null,
    vm: CreatePlaceViewModel = koinViewModel { parametersOf(groupId, placeId, lat, lng) },
) {
    val isNewPlace by vm.uiState.map { it.data?.isNewPlace }.collectAsState(initial = null)
    val pickerState = rememberEmojiBottomSheetPickerState()

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colors.background).safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = when (isNewPlace) {
                    true -> stringResource(MR.strings.places_create_screen_title)
                    false -> stringResource(MR.strings.places_edit_screen_title)
                    else -> null
                },
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        CreatePlaceScreenCore(
            appState = appState,
            vm = vm,
            onNavigateBack = { appState.navController.popBackStack() },
            onBottomSheetAction = { screen ->
                appState.uiScope.launch {
                    pickerState.show(screen.emojis)
                }
            },
            mapContent = { state ->
                PlaceCreateMapComposable(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onMapCameraSettled = { vm.onEvent(CreatePlaceScreenUiEvent.OnMapPositionChanged(it)) },
                    onMapInteraction = { },
                    onMapTap = { lat, lng -> vm.onEvent(CreatePlaceScreenUiEvent.OnMapTap(lat, lng)) },
                    onMapLongPress = { lat, lng -> vm.onEvent(CreatePlaceScreenUiEvent.OnMapTap(lat, lng)) }
                )
            }
        )
    }

    EmojiBottomSheetPicker(
        state = pickerState,
        onClick = {
            vm.onEvent(CreatePlaceScreenUiEvent.OnImageChanged(it))
            appState.uiScope.launch { pickerState.hide() }
        }
    )
}
