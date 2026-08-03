package app.paralino.core.ui.groups.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.EditImageIcon
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose

@Composable
fun ComponentWithDurationExpirationBanner(
    modifier: Modifier = Modifier,
    expirationTime: String,
    isExpired: Boolean,
    canEdit: Boolean
) {
    Row(
        modifier = modifier
            .background(
                color = if (isExpired) AppColors.errorBackground else MaterialTheme.colors.surface,
                shape = RoundedCornerShape(percent = 50)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = CoreAppIcons.Timer.asPainterResource(),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            modifier = Modifier.weight(weight = 1f, fill = canEdit).padding(end = 2.dp),
            text = expirationTime.annotatedBoldCompose(),
            style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface)
        )

        if (canEdit) {
            EditImageIcon(modifier = Modifier)
        }
    }
}