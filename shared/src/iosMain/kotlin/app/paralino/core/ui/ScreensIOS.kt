package app.paralino.core.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.core.ui.components.AppSnackbar
import app.paralino.core.ui.theme.AppTheme

object ScreensIOS {
    val login = LoginScreens
    val home = HomeScreens
}

@Composable
fun RootScreenIOS(content: @Composable (CoreUiState) -> Unit) {
    val coreUiState = rememberCoreUiState()
    AppTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = coreUiState.scaffoldState,
            snackbarHost = { AppSnackbar(it) }
        ) { innerPadding ->
            content(coreUiState)
        }
    }
}