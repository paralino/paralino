package com.zbapp.locationtrack.ui

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.zbapp.locationtrack.ui.components.BottomSheetsScreen
import com.zbapp.locationtrack.ui.components.AppSnackbarData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
class MainAppState(
    val scaffoldState: ScaffoldState,
    val bottomSheetState: BottomSheetState,
    val navController: NavHostController,
    val uiScope: CoroutineScope
) {

    fun showBottomSheet(bottomSheetsScreen: BottomSheetsScreen) {
        bottomSheetState.currentBottomSheet.value = bottomSheetsScreen
        uiScope.launch {
            bottomSheetState.bottomSheetState.show()
        }
    }

    suspend fun showBottomSheetForResult(bottomSheetsScreen: BottomSheetsScreen): Any {
        showBottomSheet(bottomSheetsScreen)
        return bottomSheetState.sheetResultChannel.receive()
    }

    fun showSnackbar(text: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        uiScope.launch {
            showSnackbar(text = text, actionText = null, duration = duration)
        }
    }

    suspend fun showSnackbar(
        text: String,
        actionText: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult {
        return scaffoldState.snackbarHostState.showSnackbar(
            message = text,
            actionLabel = actionText,
            duration = duration
        )
    }

    suspend fun showSnackbar(data: AppSnackbarData): SnackbarResult {
        return showSnackbar(
            text = data.text,
            actionText = data.actionText,
            duration = when (data.duration) {
                com.zbapp.locationtrack.ui.components.SnackbarDuration.Long -> SnackbarDuration.Long
                com.zbapp.locationtrack.ui.components.SnackbarDuration.Forever -> SnackbarDuration.Indefinite
                else -> SnackbarDuration.Short
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    bottomSheetState: BottomSheetState = rememberBottomSheet(),
    navController: NavHostController = rememberNavController(),
    uiScope: CoroutineScope = rememberCoroutineScope()
) = remember(scaffoldState, navController, bottomSheetState) {
    MainAppState(
        scaffoldState = scaffoldState,
        navController = navController,
        bottomSheetState = bottomSheetState,
        uiScope = uiScope
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberBottomSheet(
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    currentBottomSheet: MutableState<BottomSheetsScreen?> = remember {
        mutableStateOf(null)
    },
    channel: Channel<Any> = Channel()
) = remember {
    BottomSheetState(
        bottomSheetState = bottomSheetState,
        currentBottomSheet = currentBottomSheet,
        sheetResultChannel = channel
    )
}

@OptIn(ExperimentalMaterialApi::class)
class BottomSheetState(
    val bottomSheetState: ModalBottomSheetState,
    val currentBottomSheet: MutableState<BottomSheetsScreen?>,
    val sheetResultChannel: Channel<Any>
)