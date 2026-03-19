package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors

@Composable
fun EditImageIcon(
    modifier: Modifier = Modifier
) {
    Icon(
        modifier = modifier
            .size(28.dp)
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = 16.dp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                )
            )
            .clip(CircleShape)
            .background(MaterialTheme.colors.background)
            .background(
                if (isSystemInDarkTheme()) {
                    AppColors.accentLight.copy(alpha = 0.2f)
                } else {
                    AppColors.accent.copy(alpha = 0.2f)
                }
            )
            .padding(5.dp),
        painter = CoreAppIcons.Edit.asPainterResource(),
        tint = MaterialTheme.colors.onBackground,
        contentDescription = null
    )
}