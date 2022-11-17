package app.paralino.android.ui.rawdata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.DefaultEmptyState
import app.paralino.android.ui.shared.errorString
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.rawdata.RawDataScreenEvent
import app.paralino.core.presentation.rawdata.RawDataScreenState
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RawDataScreen(
    appState: MainAppState,
    type: RawPresentationDataType,
    vm: RawDataPresentationViewModel = getViewModel { parametersOf(type) }
) {

    val state by vm.uiState.collectAsState(initial = Payload.Loading)

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is RawDataScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(text = state.data?.title ?: "---") },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) {
        when (val finalState = state) {
            Payload.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            is Payload.Error -> {
                DefaultEmptyState(content = "", icon = Icons.Rounded.Warning)
            }
            is Payload.Success -> {
                RawDataScreen(
                    state = finalState.data
                )
            }
        }
    }

}

@Composable
fun RawDataScreen(
    state: RawDataScreenState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 40.dp, horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Text(text = "Encrypted data")
        Text(
            text = state.rawContent,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.size(40.dp))

        Text(text = "Decrypted data")
        Text(
            text = state.decryptedContent,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

    }
}