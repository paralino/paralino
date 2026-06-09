package app.paralino.core.ui.privacyzones

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.SuggestionSectionChipData
import app.paralino.core.ui.components.EditImageIcon
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.groups.ShakeEffect
import app.paralino.core.ui.groups.components.ChipsSuggestionsSection
import app.paralino.core.ui.theme.LocalScreenDimensionType
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch

@Composable
fun CreatePrivacyZoneNameSuggestionStep(
    modifier: Modifier = Modifier,
    focusManager: FocusManager,
    shakeImage: Boolean,
    image: ImageModel,
    name: String,
    hasNameError: Boolean,
    suggestions: List<SuggestionSectionChipData>,
    onImageClick: () -> Unit,
    onNameChanged: (String) -> Unit,
    onSuggestionSelected: (SuggestionSectionChipData) -> Unit,
) {
    var rotation by remember { mutableStateOf(0f) }
    if (shakeImage) {
        ShakeEffect {
            rotation = it
        }
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        val isSmallScreen = LocalScreenDimensionType.current.isSmall

        Box(
            modifier = Modifier.size(if (isSmallScreen) 80.dp else 120.dp),
            contentAlignment = Alignment.Center
        ) {
            ImageModelImage(
                imageModel = image,
                modifier = Modifier
                    .size(if (isSmallScreen) 70.dp else 100.dp)
                    .rotate(rotation)
                    .clickable(onClick = onImageClick)
            )
            EditImageIcon(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(onClick = onImageClick)
            )
        }

        Spacer(modifier = Modifier.size(if (isSmallScreen) 10.dp else 20.dp))

        MainInputField(
            modifier = Modifier.height(70.dp),
            text = name,
            hint = stringResource(MR.strings.privacy_zones_create_name_placeholder),
            hasError = hasNameError,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            focusManager = focusManager,
            onInputChange = onNameChanged
        )

        if (suggestions.isNotEmpty()) {
            ChipsSuggestionsSection(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                suggestions = suggestions,
                onSuggestionClick = {
                    onSuggestionSelected(it)
                    scope.launch {
                        scrollState.animateScrollTo(0)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
