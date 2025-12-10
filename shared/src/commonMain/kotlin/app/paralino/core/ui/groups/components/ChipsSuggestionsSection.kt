package app.paralino.core.ui.groups.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource

data class SuggestionSectionChipData(
    val image: ImageModel,
    val name: String,
)

@Composable
fun ChipsSuggestionsSection(
    modifier: Modifier = Modifier,
    suggestions: List<SuggestionSectionChipData>,
    onSuggestionClick: (SuggestionSectionChipData) -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {

        DataInfoChip(
            modifier = Modifier.padding(bottom = 4.dp).fillMaxWidth(),
            imageOrIcon = {
                ImageModelImage(
                    imageModel = ImageModel.emoji(Emojis.lightBulb),
                    modifier = Modifier.size(16.dp),
                    useBackgroundForEmoji = false,
                    clipShape = CircleShape
                )
                Spacer(Modifier.size(4.dp))
            },
            text = {
                Text(
                    text = stringResource(MR.strings.group_create_suggestions_header),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f)
                )
            },
            backgroundColor = Color.Transparent,
            hasEndArrow = false,
            onClick = null
        )

        suggestions.forEach { suggestion ->
            DataInfoChip(
                image = suggestion.image,
                text = suggestion.name,
                onClick = { onSuggestionClick(suggestion) }
            )
        }
    }
}