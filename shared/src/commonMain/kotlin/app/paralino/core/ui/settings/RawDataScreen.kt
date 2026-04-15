package app.paralino.core.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import app.paralino.core.domain.crypto.ContentIdItem
import app.paralino.core.models.Payload
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.rawdata.RawDataScreenState
import app.paralino.core.presentation.rawdata.RawDataScreenUiEvent
import app.paralino.core.presentation.rawdata.RawDataSectionState
import app.paralino.core.ui.CoreUiState
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun RawDataScreenCore(
    appState: CoreUiState,
    vm: RawDataPresentationViewModel,
    paddingValues: PaddingValues
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState(initial = Payload.Loading)

    when (val finalState = state) {
        Payload.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
                state = finalState.data,
                onCopy = { vm.onEvent(RawDataScreenUiEvent.CopyContent(it)) }
            )
        }
    }
}

@Composable
fun RawDataScreen(
    state: RawDataScreenState,
    onCopy: (ContentIdItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 60.dp, start = 20.dp, end = 20.dp)
    ) {
        this.item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
        }

        this.item(key = "title") {
            if (state.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.size(40.dp))
            } else {
                Text(
                    modifier = Modifier.padding(
                        top = 30.dp
                    ),
                    text = state.subtitle,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
                )
            }
        }

        this.itemsIndexed(state.sections) { index, section ->
            if (!state.subtitle.isNullOrBlank() && index == 0) {
                if (section.title != null) {
                    Spacer(modifier = Modifier.size(40.dp))
                } else {
                    Spacer(modifier = Modifier.size(10.dp))
                }
            }

            if (index > 0) {
                Spacer(modifier = Modifier.size(40.dp))
            }

            RawDataScreenSection(section = section, onCopy = onCopy)
        }
    }
}

@Composable
private fun RawDataScreenSection(
    section: RawDataSectionState,
    onCopy: (ContentIdItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (section.title != null) {
            Text(
                text = section.title.annotatedBoldCompose(),
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )
        }

        if (section.subtitle != null) {
            Text(
                text = section.subtitle.annotatedBoldCompose(),
                style = MaterialTheme.typography.subtitle2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 0.dp)
            )
        }

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
            section.contentIdInfo.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = if(item.separateWithPadding) 4.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (item.canCopy) {
                        Icon(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onCopy(item) }
                                .padding(vertical = 4.dp)
                                .padding(end = 8.dp),
                            painter = CoreAppIcons.ContentCopy.asPainterResource(),
                            contentDescription = null
                        )
                    }
                    Text(
                        modifier = Modifier,
                        text = item.text.annotatedBoldCompose(),
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Light)
                    )
                }
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

        if (section.decryptedContentSubtitle != null) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp, start = 8.dp),
                    text = section.decryptedContentSubtitle.annotatedBoldCompose(),
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Light)
                )
            }
        }

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
            text = text.annotatedBoldCompose(),
            style = MaterialTheme.typography.caption
        )
    }
}