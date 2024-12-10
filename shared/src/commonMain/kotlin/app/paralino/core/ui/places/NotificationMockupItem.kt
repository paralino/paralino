package app.paralino.core.ui.places

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.utils.extensions.annotatedBold
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun NotificationMockupItem(
    modifier: Modifier = Modifier,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(vertical = 8.dp, horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = CoreAppIcons.LauncherLogo.asPainterResource(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colors.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    modifier = Modifier,
                    text = stringResource(MR.strings.app_name),
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Start
                )
                Text(
                    modifier = Modifier,
                    text = description.annotatedBold(),
                    style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Start
                )
            }
            Icon(
                modifier = Modifier.width(24.dp),
                painter = CoreAppIcons.NotificationsActive.asPainterResource(),
                tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                contentDescription = null
            )
        }
    }
}