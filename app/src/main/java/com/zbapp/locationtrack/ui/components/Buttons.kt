package com.zbapp.locationtrack.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zbapp.locationtrack.ui.theme.ButtonStyle

@Composable
fun FilledButton(
    modifier: Modifier = Modifier,
    style: ButtonStyle,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        modifier = style.defaultModifier.then(modifier),
        contentPadding = style.paddingValues,
        onClick = onClick,
        content = {
            if (icon != null) {
                Icon(imageVector = icon, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(8.dp))
            }
            content()
        }
    )
}