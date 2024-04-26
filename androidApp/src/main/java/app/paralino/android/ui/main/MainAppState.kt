package app.paralino.android.ui.main

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.CoreUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
class MainAppState(
    scaffoldState: ScaffoldState,
    val bottomSheetState: BottomSheetState,
    val navController: NavHostController,
    uiScope: CoroutineScope
): CoreUiState(scaffoldState, uiScope) {

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
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true),
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