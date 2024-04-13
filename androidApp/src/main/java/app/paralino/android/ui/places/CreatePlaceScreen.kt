package app.paralino.android.ui.places

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.PlaceCreateMapComposable
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.places.CreatePlaceScreenCore
import app.paralino.core.utils.extensions.safeCast
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CreatePlaceScreen(
    appState: MainAppState,
    groupId: String,
    placeId: String?,
    lat: Double? = null,
    lng: Double? = null,
    vm: CreatePlaceViewModel = getViewModel { parametersOf(groupId, placeId, lat, lng) },
) {
    val isNewPlace by vm.uiState.map { it.data?.isNewPlace }.collectAsState(initial = null)
    Scaffold(
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
                    if (screen != null) {
                        appState.showBottomSheetForResult(screen)
                            .safeCast<ImageModel>()?.let {
                                vm.updateImage(it)
                            }
                    }
                }
            },
            mapContent = { state ->
                PlaceCreateMapComposable(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onMapInteraction = { },
                    onMapLongPress = { lat, lng -> vm.updatePosition(lat, lng) }
                )
            }
        )
    }
}
