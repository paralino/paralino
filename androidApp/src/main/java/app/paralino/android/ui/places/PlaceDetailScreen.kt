package app.paralino.android.ui.places

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.map.PlaceDetailMapComposable
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.presentation.places.detail.PlaceSettingsState
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.places.PlaceDetailScreenCore
import app.paralino.core.ui.places.PlaceDetailUserListItem
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.Emojis
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PlaceDetailScreen(
    appState: MainAppState,
    groupId: String,
    placeId: String,
    vm: PlaceDetailViewModel = getViewModel { parametersOf(groupId, placeId) },
) {

    var userScroll by remember {
        mutableStateOf(true)
    }

    Scaffold(
        topBar = {
            DefaultToolbar(
                title = null,
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        PlaceDetailScreenCore(
            appState = appState,
            vm = vm,
            userScrollEnabled = userScroll,
            contentPadding = remember {
                PaddingValues()
            },
            onNavigateBack = {
                appState.navController.popBackStack()
            },
            openPlaceRawDataScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.RawDataScreen.route(
                        RawPresentationDataType.GroupPlacePresentation(
                            placeId = placeId
                        )
                    )
                )
            },
            openPlaceEditScreen = {
                appState.navController.navigate(
                    AppScreen.MainHome.PlaceCreate.route(
                        groupId = groupId,
                        placeId = placeId
                    )
                )
            },
            mapContent = { state ->
                PlaceDetailMapComposable(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onMapInteraction = { userScroll = !it }
                )
            }
        )
    }
}

@Preview("place_detail_user_item")
@Composable
private fun PlaceDetailUserItemPreview() {
    AppTheme {
        Column {
            val item = PlaceSettingsState(
                groupId = "",
                placeId = "",
                memberUserId = "",
                isCurrentUser = false,
                name = "Some user",
                image = ImageModel.emoji(Emojis.lightBulb),
                notifyEnter = false,
                notifyExit = false,
                loadingEnter = false,
                loadingExit = false
            )
            PlaceDetailUserListItem(
                state = item,
                onEnterChange = {},
                onExitChange = {}
            )
            PlaceDetailUserListItem(
                state = item.copy(notifyEnter = true, notifyExit = true),
                onEnterChange = {},
                onExitChange = {}
            )
            PlaceDetailUserListItem(
                state = item.copy(notifyEnter = true, loadingEnter = true),
                onEnterChange = {},
                onExitChange = {}
            )
            PlaceDetailUserListItem(
                state = item.copy(loadingExit = true, isCurrentUser = true),
                onEnterChange = {},
                onExitChange = {}
            )
        }
    }
}