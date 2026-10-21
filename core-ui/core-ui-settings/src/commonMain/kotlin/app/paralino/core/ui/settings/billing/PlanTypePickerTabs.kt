package app.paralino.core.ui.settings.billing

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.presentation.settings.billing.PaywallScreenState
import app.paralino.core.presentation.settings.billing.PaywallScreenUiEvent


@Composable
internal fun ColumnScope.PlanTypePickerTabs(
    modifier: Modifier = Modifier,
    state: PaywallScreenState,
    onEvent: (PaywallScreenUiEvent) -> Unit
) {
    val plans = state.plans
    if (plans.isEmpty()) return

    val density = LocalDensity.current
    var containerWidth by remember { mutableFloatStateOf(0f) }
    val selectedIndex = plans.indexOfFirst { it.isSelected }
    val segmentWidth = with(density) { (containerWidth / plans.size).toDp() }
    val pillOffset by animateDpAsState(segmentWidth * selectedIndex.coerceAtLeast(0))
    val pillAlpha by animateFloatAsState(if (selectedIndex >= 0) 1f else 0f)
    val indication = LocalIndication.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(56.dp)
            .align(Alignment.CenterHorizontally)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.surface)
            .padding(4.dp)
            .onSizeChanged { size ->
                containerWidth = size.width.toFloat()
            }
    ) {
        // sliding selection indicator, all segments keep equal width
        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .alpha(pillAlpha)
                .indication(
                    interactionSource = interactionSource,
                    indication = indication,
                )
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.primary)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            plans.forEach { plan ->
                PlanName(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = if (plan.isSelected) interactionSource else remember { MutableInteractionSource() },
                            indication = if (plan.isSelected) indication else LocalIndication.current,
                        ) {
                            onEvent(PaywallScreenUiEvent.SelectPlanType(plan.type))
                        },
                    selected = plan.isSelected,
                    text = plan.displayedName
                )
            }
        }
    }
}


@Composable
private fun PlanName(
    modifier: Modifier = Modifier,
    selected: Boolean,
    text: String
) {
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colors.onPrimary
        else MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            color = contentColor,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}