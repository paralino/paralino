package app.paralino.android.ui.places

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.map.PlaceCreateMapComposable
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.ui.places.CreatePlaceScreenCore
import app.paralino.core.utils.extensions.safeCast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CreatePlaceScreen(
    appState: MainAppState,
    groupId: String,
    placeId: String?,
    lat: Double? = null,
    lng: Double? = null,
    vm: CreatePlaceViewModel = getViewModel { parametersOf(groupId, placeId, lat, lng) },
) {

    var userScroll by remember {
        mutableStateOf(true)
    }

    CreatePlaceScreenCore(
        appState = appState,
        vm = vm,
        userScrollEnabled = userScroll,
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
                onMapInteraction = { userScroll = !it },
                onMapLongPress = { lat, lng -> vm.updatePosition(lat, lng) }
            )
        }
    )
}
