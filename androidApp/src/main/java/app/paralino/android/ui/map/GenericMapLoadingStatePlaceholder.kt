package app.paralino.android.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource

@Composable
fun BoxScope.GenericMapLoadingStatePlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.fillMaxHeight(0.3f))
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
    }
}