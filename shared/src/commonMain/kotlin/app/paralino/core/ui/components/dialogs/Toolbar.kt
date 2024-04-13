package app.paralino.core.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource

@Composable
fun DefaultToolbar(
    title: String?,
    navigationIcon: @Composable () -> Unit
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.background,
        elevation = 4.dp,
        title = {
            if (title != null) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = navigationIcon
    )
}

object ToolbarButtons {
    @Composable
    fun Back(onClick: () -> Unit) {
        Icon(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(40.dp)
                .clickable { onClick() }
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp),
            painter = CoreAppIcons.ChevronLeft.asPainterResource(),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface
        )
    }

    @Composable
    fun Close(onClick: () -> Unit) {
        Icon(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(40.dp)
                .clickable { onClick() }
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(10.dp),
            painter = CoreAppIcons.Close.asPainterResource(),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface
        )
    }
}