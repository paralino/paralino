package app.paralino.core.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.rawdata.RawDataScreenEvent
import app.paralino.core.presentation.rawdata.RawDataScreenState
import app.paralino.core.presentation.rawdata.RawDataSectionState
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.utils.extensions.errorString
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RawDataScreenCore(
    appState: CoreUiState,
    vm: RawDataPresentationViewModel,
    onNavigateBack: () -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState(initial = Payload.Loading)

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is RawDataScreenEvent.Error -> {
                    vm.showSnackbar(event.error.errorString())
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
                title = { Text(text = stringResource(MR.strings.raw_data_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            painter = CoreAppIcons.Close.asPainterResource(),
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
                DefaultEmptyState(content = "", icon = CoreAppIcons.Warning.asPainterResource())
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
            .padding(vertical = 20.dp, horizontal = 20.dp)
            .padding(bottom = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(12.dp))
                .background(
                    color = MaterialTheme.colors.background,
                    shape = RoundedCornerShape(size = 12.dp)
                )
                .background(
                    color = AppColors.accentLight.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(size = 12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                painter = CoreAppIcons.Security.asPainterResource(),
                contentDescription = null
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(MR.strings.raw_data_screen_info),
                style = MaterialTheme.typography.caption,
            )
        }

        state.sections.forEach {
            Spacer(modifier = Modifier.size(40.dp))
            RawDataScreenSection(it)
        }
    }
}

@Composable
private fun RawDataScreenSection(
    section: RawDataSectionState
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (section.title != null) {
            Text(
                text = stringResource(
                    MR.strings.raw_data_screen_section_info,
                    section.title
                ).annotatedBold(),
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )
        }

        if (section.subtitle != null) {
            Text(
                text = stringResource(
                    MR.strings.raw_data_screen_section_info,
                    section.subtitle
                ).annotatedBold(),
                style = MaterialTheme.typography.subtitle2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 0.dp)
            )
        }

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
            section.contentIdInfo.forEach {
                Text(
                    modifier = Modifier,
                    text = it.annotatedBold(),
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Light)
                )
            }
        }

        Spacer(modifier = Modifier.size(20.dp))

        RawDataChip(
            modifier = Modifier.padding(bottom = 8.dp),
            icon = CoreAppIcons.Lock.asPainterResource(),
            text = stringResource(MR.strings.raw_data_section_encrypted)
        )

        Text(
            text = section.rawContent,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.size(4.dp))

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
            Icon(
                modifier = Modifier.padding(start = 10.dp),
                painter = CoreAppIcons.ExpandMore.asPainterResource(),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.size(4.dp))

        RawDataChip(
            modifier = Modifier.padding(bottom = 8.dp),
            icon = CoreAppIcons.LockOpen.asPainterResource(),
            text = stringResource(MR.strings.raw_data_section_decrypted)
        )

        Text(
            text = section.decryptedContent,
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
    icon: Painter,
    text: String
) {
    Row(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(percent = 50))
            .background(
                color = MaterialTheme.colors.background,
                shape = RoundedCornerShape(percent = 50)
            )
            .background(
                color = AppColors.accentLight.copy(alpha = 0.3f),
                shape = RoundedCornerShape(percent = 50)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
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