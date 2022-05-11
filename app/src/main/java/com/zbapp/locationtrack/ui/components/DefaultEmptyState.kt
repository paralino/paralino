package com.zbapp.locationtrack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import com.zbapp.locationtrack.utils.extensions.annotatedBold

@Composable
fun DefaultEmptyState(
    modifier: Modifier = Modifier,
    content: String,
    icon: ImageVector? = null,
    buttonText: String? = null,
    buttonIcon: ImageVector? = null,
    bigButton: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Spacer(modifier = Modifier.size(20.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .alpha(ContentAlpha.disabled)
            )
        }

        Spacer(modifier = Modifier.size(50.dp))

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(text = content.annotatedBold(), textAlign = TextAlign.Center)
        }

        if (buttonText != null) {
            Spacer(modifier = Modifier.size(50.dp))
            FilledButton(
                style = if (bigButton) ButtonStyle.BIG else ButtonStyle.SMALL,
                icon = buttonIcon,
                onClick = onClick ?: { }
            ) {
                Text(buttonText)
            }
        }
    }
}