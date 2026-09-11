package app.paralino.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.utils.extensions.annotatedBoldCompose
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

data class ComponentInfoPoint(
    val title: String? = null,
    val content: String,
)

@Composable
fun ComponentInfoBox(
    modifier: Modifier = Modifier,
    items: List<ComponentInfoPoint>,
    footerInfoText: String?,
    onCopy: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colors.surface,
                shape = MaterialTheme.shapes.medium
            )
            .background(
                color = MaterialTheme.colors.surface.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            ComponentInfoPointRow(
                item = item,
                onContentCopy = onCopy
            )
        }

        if (footerInfoText != null) {
            Column {
                Spacer(Modifier.size(4.dp))
                Divider(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = Color.Gray.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )
                Spacer(Modifier.size(10.dp))
                if (footerInfoText.isNotBlank()) {
                    CompositionLocalProvider(LocalContentAlpha provides 0.6f) {
                        Text(
                            text = footerInfoText.annotatedBoldCompose(),
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentInfoPointRow(
    modifier: Modifier = Modifier,
    item: ComponentInfoPoint,
    onContentCopy: ((String) -> Unit)?
) {
    var copyIconConfirmation by remember { mutableStateOf(false) }
    LaunchedEffect(copyIconConfirmation) {
        if (copyIconConfirmation) {
            delay(1.seconds)
            copyIconConfirmation = false
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (onContentCopy != null) {
            AnimatedContent(
                targetState = if (copyIconConfirmation) {
                    CoreAppIcons.Check
                } else {
                    CoreAppIcons.ContentCopy
                },
                content = { icon ->
                    Icon(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(32.dp)
                            .clickable {
                                onContentCopy(item.content)
                                copyIconConfirmation = true
                            }
                            .padding(vertical = 4.dp),
                        painter = icon.asPainterResource(),
                        contentDescription = null,
                        tint = MaterialTheme.colors.onBackground.copy(alpha = 0.8f)
                    )
                }
            )
        }
        CompositionLocalProvider(LocalContentAlpha provides 0.6f) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (item.title != null) {
                    Text(
                        modifier = Modifier,
                        text = item.title,
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    modifier = Modifier,
                    text = item.content.annotatedBoldCompose(),
                    style = MaterialTheme.typography.overline.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.8.sp,
                        lineHeight = 14.sp
                    )
                )
            }
        }
    }
}
