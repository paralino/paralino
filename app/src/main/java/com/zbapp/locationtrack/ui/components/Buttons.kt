package com.zbapp.locationtrack.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zbapp.locationtrack.ui.theme.ButtonStyle

@Composable
fun FilledButton(
    modifier: Modifier = Modifier,
    style: ButtonStyle,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        modifier = style.defaultModifier.then(modifier),
        contentPadding = style.paddingValues,
        onClick = onClick,
        content = content
    )
}