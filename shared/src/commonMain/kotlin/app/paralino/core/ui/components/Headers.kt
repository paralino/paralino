package app.paralino.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.utils.extensions.annotatedBold

@Composable
fun SettingsBigHeader(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 20.dp,
    name: String
) {
    Text(
        text = name.annotatedBold(),
        style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold),
        modifier = modifier
            .padding(
                top = 20.dp,
                bottom = bottomPadding
            )
    )
}

@Composable
fun SettingsSectionHeader(
    modifier: Modifier = Modifier,
    name: String
) {
    Text(
        text = name.annotatedBold(),
        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Light),
        modifier = modifier
            .padding(
                top = 20.dp,
                bottom = 8.dp
            )
            .alpha(ContentAlpha.medium)
    )
}