package app.paralino.android.ui.rawdata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.DefaultEmptyState
import app.paralino.android.ui.shared.errorString
import app.paralino.core.ui.theme.AppColors
import app.paralino.android.ui.theme.AppIcons
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
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
                            imageVector = AppIcons.Close,
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
                DefaultEmptyState(content = "", icon = AppIcons.Warning)
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
            .verticalScroll(rememberScrollState())
            .padding(vertical = 40.dp, horizontal = 20.dp),
    ) {

        Text(
            text = stringResource(id = R.string.raw_data_screen_info),
            style = MaterialTheme.typography.caption,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        )

        RawDataChip(
            modifier = Modifier.padding(bottom = 8.dp),
            icon = AppIcons.Lock,
            text = stringResource(R.string.raw_data_section_encrypted)
        )
        Text(
            text = state.rawContent,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.size(40.dp))

        RawDataChip(
            modifier = Modifier.padding(bottom = 8.dp),
            icon = AppIcons.LockOpen,
            text = stringResource(R.string.raw_data_section_decrypted)
        )
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

@Composable
private fun RawDataChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = modifier
            .background(
                AppColors.accentLight.copy(alpha = 0.3f),
                RoundedCornerShape(percent = 50)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = text.annotatedBold(),
            style = MaterialTheme.typography.caption
        )
    }
}