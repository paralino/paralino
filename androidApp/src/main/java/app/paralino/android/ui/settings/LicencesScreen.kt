package app.paralino.android.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.theme.AppIcons
import app.paralino.core.MR
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer

@Composable
fun LicencesScreen(appState: MainAppState) {

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(text = stringResource(id = MR.strings.setting_title_libraries.resourceId)) },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            painter = AppIcons.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) {
        LibrariesContainer(
            modifier = Modifier.fillMaxSize(),
            contentPadding = it
        )
    }

}