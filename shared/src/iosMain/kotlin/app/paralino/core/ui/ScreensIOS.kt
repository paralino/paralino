package app.paralino.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.components.AppSnackbar
import app.paralino.core.ui.theme.AppTheme

object ScreensIOS {
    val login = LoginScreens
    val home = HomeScreens
    val settings = SettingsScreens
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

data class AppleScreenPaddingValues(
    val start: Float,
    val top: Float,
    val end: Float,
    val bottom: Float
) {
    fun toComposePaddingValues() = PaddingValues(
        start = start.dp,
        top = top.dp,
        end = end.dp,
        bottom = bottom.dp
    )
}