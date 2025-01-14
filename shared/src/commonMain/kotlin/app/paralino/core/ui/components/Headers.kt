package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
    name: String,
    backgroundColor: Color = MaterialTheme.colors.background,
    count: Int? = null,
    limit: Int? = null,
    end: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .padding(top = 20.dp, bottom = 8.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.alpha(ContentAlpha.medium),
            text = name,
            style = MaterialTheme.typography.h6,
        )
        if (count != null) {
            Spacer(modifier = Modifier.size(8.dp))
            PlanLimitCount(count = count, limit = limit)
        }
        if (end != null) {
            Spacer(modifier = Modifier.weight(1f))
            end()
        }
    }
}