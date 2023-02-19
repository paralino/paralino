package app.paralino.android.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.theme.AppIcons
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer

@Composable
fun LicencesScreen(appState: MainAppState) {

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(text = stringResource(id = R.string.setting_title_libraries)) },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            imageVector = AppIcons.Close,
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