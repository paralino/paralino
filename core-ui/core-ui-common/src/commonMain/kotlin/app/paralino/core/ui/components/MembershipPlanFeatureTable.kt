package app.paralino.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.presentation.billing.FeatureTableItemHighlightMode
import app.paralino.core.presentation.billing.FeatureTableItemState
import app.paralino.core.presentation.billing.FeatureTableItemValue
import app.paralino.core.presentation.billing.FeatureTableItemVisibilityMode
import app.paralino.core.presentation.billing.FeatureTableState
import app.paralino.core.presentation.billing.FeatureTableSummary
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun MembershipPlanFeatureTable(
    modifier: Modifier = Modifier,
    table: FeatureTableState,
    canExpand: Boolean = table.canExpand,
    isExpanded: Boolean,
    onExpandPlans: () -> Unit,
    onCellClick: (FeatureTableItemState, FeatureTableItemValue) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isExpanded) {
                    MaterialTheme.colors.surface
                } else {
                    Color.Transparent
                }
            )
            .padding(animateDpAsState(if (isExpanded) 4.dp else 0.dp).value)
            .clip(RoundedCornerShape(10.dp))
    ) {
        val density = LocalDensity.current
        val maxWidth = this.maxWidth
        val scrollState = rememberScrollState()
        var containerWidthDp by remember { mutableStateOf(0.dp) }

        Column(
            modifier = Modifier
        ) {
            Column(
                modifier = Modifier
                    .animateContentSize()
                    .horizontalScroll(scrollState)
                    .onSizeChanged { size ->
                        containerWidthDp = with(density) { size.width.toDp() }
                    }
            ) {
                table.features
                    .filter { feature ->
                        when (feature.visibilityMode) {
                            FeatureTableItemVisibilityMode.VISIBLE_ALWAYS -> true
                            FeatureTableItemVisibilityMode.VISIBLE_EXPANDED -> isExpanded
                        }
                    }
                    .forEachIndexed { index, feature ->
                        Box {
                            FeatureTableItem(
                                modifier = Modifier,
                                featureItem = feature,
                                index = index,
                                maxWidth = maxWidth,
                                isExpanded = isExpanded,
                                onCellClick = onCellClick
                            )

                            if (index > 1) {
                                Divider(
                                    modifier = Modifier
                                        // having horizontal padding here for some reason breaks table width
                                        // start padding + smaller width makes it render properly
                                        .width(containerWidthDp - 8.dp)
                                        .padding(start = 8.dp),
                                    color = Color.Gray.copy(alpha = 0.2f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
            }

            AnimatedVisibility(visible = canExpand) {
                BenefitsExpandButton(
                    modifier = Modifier
                        .padding(bottom = 8.dp, top = 16.dp)
                        .padding(horizontal = 8.dp),
                    text = stringResource(MR.strings.paywall_feature_table_expand_button),
                    onClick = onExpandPlans
                )
            }
        }
    }
}

@Composable
private fun FeatureTableItem(
    modifier: Modifier = Modifier,
    featureItem: FeatureTableItemState,
    index: Int,
    maxWidth: Dp,
    isExpanded: Boolean,
    onCellClick: (FeatureTableItemState, FeatureTableItemValue) -> Unit
) {
    var containerHeight by remember(featureItem) { mutableStateOf(0f) }
    val nameWidth = when (featureItem) {
        is FeatureTableItemState.Section -> maxWidth
        is FeatureTableItemState.Title,
        is FeatureTableItemState.Detail -> {
            if (featureItem.values.isEmpty()) {
                maxWidth
            } else if (featureItem.values.size == 1) {
                if (isExpanded) {
                    maxWidth * 0.7f
                } else {
                    maxWidth * 0.8f
                }
            } else if (featureItem.values.size == 2) {
                maxWidth * 0.6f
            } else {
                maxWidth * 0.5f
            }
        }
    }

    val valueWidth = when (featureItem) {
        is FeatureTableItemState.Section -> 0.dp
        is FeatureTableItemState.Title,
        is FeatureTableItemState.Detail -> {
            if (featureItem.values.isEmpty()) {
                0.dp
            } else if (featureItem.values.size == 1) {
                if (isExpanded) {
                    maxWidth * 0.3f
                } else {
                    maxWidth * 0.2f
                }
            } else if (featureItem.values.size == 2) {
                maxWidth * 0.2f
            } else {
                maxWidth * 0.2f
            }
        }
    }

    Row(
        modifier = modifier
            .defaultMinSize(
                minHeight = when (featureItem) {
                    is FeatureTableItemState.Detail,
                    is FeatureTableItemState.Title -> 34.dp

                    is FeatureTableItemState.Section -> 20.dp
                }
            )
//            .conditional(featureItem is FeatureTableItemState.Detail) {
//                background(
//                    color = AppColors.accentLight.copy(alpha = if (index % 2 == 0) 0.1f else 0f),
//                    shape = RoundedCornerShape(8.dp)
//                )
//            }
            .onSizeChanged { size ->
                containerHeight = size.height.toFloat()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .width(nameWidth)
                .padding(start = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            when (featureItem) {
                is FeatureTableItemState.Section -> {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = featureItem.topSpacing, bottom = 4.dp),
                        text = featureItem.content.annotatedBoldCompose(),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                    )
                }

                is FeatureTableItemState.Detail -> {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        text = featureItem.content.annotatedBoldCompose(),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.caption.copy(
                            lineHeight = 14.sp,
                            hyphens = Hyphens.Auto
                        )
                    )
                }

                is FeatureTableItemState.Title -> {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                        text = featureItem.content.annotatedBoldCompose(),
                        textAlign = if (featureItem.values.isEmpty()) TextAlign.Center else TextAlign.Start,
                        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        when (featureItem) {
            is FeatureTableItemState.Section -> {}
            is FeatureTableItemState.Detail,
            is FeatureTableItemState.Title -> {
                featureItem.values.forEach { value ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (value.highlightMode) {
                                    FeatureTableItemHighlightMode.TOP,
                                    FeatureTableItemHighlightMode.BOTTOM,
                                    FeatureTableItemHighlightMode.SINGLE,
                                    FeatureTableItemHighlightMode.FULL -> AppColors.accentDark.copy(
                                        alpha = 0.2f
                                    )

                                    else -> Color.Transparent
                                },
                                shape = when (value.highlightMode) {
                                    FeatureTableItemHighlightMode.NONE -> RectangleShape
                                    FeatureTableItemHighlightMode.FULL -> RectangleShape
                                    FeatureTableItemHighlightMode.SINGLE -> RoundedCornerShape(10.dp)
                                    FeatureTableItemHighlightMode.TOP -> RoundedCornerShape(
                                        topStart = 10.dp,
                                        topEnd = 10.dp
                                    )

                                    FeatureTableItemHighlightMode.BOTTOM -> RoundedCornerShape(
                                        bottomStart = 10.dp,
                                        bottomEnd = 10.dp
                                    )
                                }
                            )
                            .width(valueWidth)
                            .defaultMinSize(minHeight = with(LocalDensity.current) { containerHeight.toDp() }),
                        contentAlignment = Alignment.Center
                    ) {
                        when (value) {
                            is FeatureTableItemValue.Count -> {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    text = value.content,
                                    textAlign = TextAlign.Center,
                                    style = if (value.isPremium) {
                                        MaterialTheme.typography.caption.copy(fontWeight = FontWeight.ExtraBold)
                                    } else {
                                        MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium)
                                    }
                                )
                            }

                            is FeatureTableItemValue.Emoji -> {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    text = value.content,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.body2
                                )
                            }

                            is FeatureTableItemValue.MoreInfo -> {
                                Icon(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { onCellClick(featureItem, value) }
                                        .padding(2.dp),
                                    painter = CoreAppIcons.InfoCircle.asPainterResource(),
                                    contentDescription = null,
                                    tint = if (isSystemInDarkTheme()) AppColors.accent else AppColors.accentDark
                                )
                            }

                            is FeatureTableItemValue.Summary -> {}

                            is FeatureTableItemValue.Text -> {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    text = value.content,
                                    textAlign = TextAlign.Center,
                                    style = if (value.isPremium) {
                                        MaterialTheme.typography.overline.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.4.sp
                                        )
                                    } else {
                                        MaterialTheme.typography.overline.copy(
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 0.4.sp
                                        )
                                    }
                                )
                            }

                            is FeatureTableItemValue.Title -> {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    text = value.content,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitsExpandButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    FilledButton(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            ),
        style = remember {
            ButtonStyle.BIG(
                paddingValues = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                shape = RoundedCornerShape(10.dp)
            )
        },
        state = AppButtonState.CONTENT,
        icon = null,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onBackground
        ),
        elevation = 0.dp,
        onClick = onClick
    ) {
        Text(text = text)
        Icon(
            modifier = Modifier
                .padding(start = 4.dp)
                .width(20.dp),
            painter = CoreAppIcons.ChevronRight.asPainterResource(),
            tint = MaterialTheme.colors.onSurface,
            contentDescription = null
        )
    }
}

@Composable
fun MembershipPlanSummaryTable(
    modifier: Modifier = Modifier,
    content: FeatureTableSummary,
    onExpandClick: (() -> Unit)?
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.background)
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        content.values.forEachIndexed { index, section ->
            if (index > 0) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = Color.Gray.copy(alpha = 0.15f),
                        thickness = 0.5.dp
                    )
                    Icon(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colors.surface)
                            .padding(2.dp),
                        painter = CoreAppIcons.Add.asPainterResource(),
                        tint = MaterialTheme.colors.onSurface,
                        contentDescription = null
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = Color.Gray.copy(alpha = 0.15f),
                        thickness = 0.5.dp
                    )
                }
            }

            PlanSummarySection(
                section = section
            )
        }

        if (onExpandClick != null) {
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.Gray.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
            BenefitsExpandButton(
                modifier = Modifier,
                text = stringResource(MR.strings.paywall_feature_table_show_all_button),
                onClick = onExpandClick
            )
        }
    }
}

@Composable
private fun PlanSummarySection(
    section: FeatureTableItemValue.Summary
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
            painter = section.icon.asPainterResource(),
            contentDescription = null,
            tint = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.disabled)
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = section.title,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                section.features.forEachIndexed { index, item ->
                    Text(
                        text = item.content,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
                    )

                    if (index < section.features.size - 1) {
                        Text(
                            text = "·",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
                        )
                    }
                }
            }
        }
    }
}