package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.utils.extensions.annotatedBold
import dev.icerock.moko.resources.compose.stringResource

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
    count: Int? = null,
    limit: Int? = null
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
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
    }
}