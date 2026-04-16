package app.paralino.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBoldCompose
import app.paralino.core.utils.extensions.conditional
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun BaseOnboardingScreen(
    modifier: Modifier = Modifier,
    order: Int,
    title: String,
    info: String?,
    buttonText: String,
    skipButtonText: String?,
    contentSizeRatio: Float,
    minHeaderSize: Dp,
    onNext: () -> Unit,
    onSkip: (() -> Unit)?,
    headerContent: @Composable () -> Unit
) {
    BaseOnboardingScreen(
        modifier = modifier,
        buttonText = buttonText,
        skipButtonText = skipButtonText ?: stringResource(MR.strings.button_skip),
        onNext = onNext,
        onSkip = onSkip
    ) {
        BaseOnboardingScrollableContent(
            order = order,
            title = title,
            info = info,
            contentSizeRatio = contentSizeRatio,
            minHeaderSize = minHeaderSize,
            paddingValues = it,
            innerPaddingValues = remember { PaddingValues() },
            headerContent = headerContent
        )
    }
}

@Composable
fun BaseOnboardingScreen(
    modifier: Modifier = Modifier,
    buttonText: String = stringResource(MR.strings.button_next),
    skipButtonText: String = stringResource(MR.strings.button_skip),
    onNext: (() -> Unit)?,
    onSkip: (() -> Unit)?,
    content: @Composable (PaddingValues) -> Unit
) {
    BaseOnboardingScreen(
        modifier = modifier,
        nextButton = if (onNext != null) {
            @Composable {
                FilledButton(
                    modifier = Modifier,
                    style = ButtonStyle.BIG,
                    state = AppButtonState.CONTENT,
                    icon = null,
                    onClick = onNext
                ) {
                    Text(text = buttonText)
                }
            }
        } else {
            null
        },
        skipButton = if (onSkip != null) {
            @Composable {
                OnboardingSkipButton(skipButtonText = skipButtonText, onSkip = onSkip)
            }
        } else {
            null
        },
        content = content
    )
}

@Composable
fun BaseOnboardingScreen(
    modifier: Modifier = Modifier,
    nextButton: (@Composable () -> Unit)?,
    skipButton: (@Composable () -> Unit)?,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (nextButton != null || skipButton != null) {
                ScaffoldBottomBarButtons(
                    paddingValues = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = animateDpAsState(if (skipButton != null) 6.dp else 20.dp).value
                    )
                ) {
                    AnimatedVisibility(
                        visible = nextButton != null
                    ) {
                        nextButton?.invoke()
                    }
                    AnimatedVisibility(
                        visible = skipButton != null
                    ) {
                        skipButton?.invoke()
                    }
                }
            }
        },
        content = content
    )
}

@Composable
fun BaseOnboardingScrollableContent(
    modifier: Modifier = Modifier,
    order: Int,
    title: String,
    info: String?,
    contentSizeRatio: Float,
    minHeaderSize: Dp,
    paddingValues: PaddingValues,
    innerPaddingValues: PaddingValues,
    headerContent: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPaddingValues),
            verticalArrangement = Arrangement.Top
        ) {
            BaseOnboardingContent(
                modifier = Modifier,
                order = order,
                title = title,
                info = info,
                contentSizeRatio = contentSizeRatio,
                minHeaderSize = minHeaderSize,
                containerHeight = this@BoxWithConstraints.minHeight,
                headerContent = headerContent
            )
        }
    }
}

@Composable
fun ColumnScope.BaseOnboardingContent(
    modifier: Modifier = Modifier,
    order: Int,
    title: String,
    info: String?,
    contentSizeRatio: Float,
    containerHeight: Dp,
    minHeaderSize: Dp,
    headerContent: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .conditional(minHeaderSize != Dp.Unspecified) {
                defaultMinSize(minHeight = minHeaderSize)
                    .height(containerHeight * (1f - contentSizeRatio))
            }
    ) {
        headerContent()
    }

    Spacer(modifier.weight(1f))

    if (contentSizeRatio > 0f) {
        Column(
            modifier = Modifier
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = order,
                label = "animated title",
                transitionSpec = onboardingContentTransitionSpec()
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            if (info != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedContent(
                    targetState = order,
                    label = "animated info",
                    transitionSpec = onboardingContentTransitionSpec()
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = info.annotatedBoldCompose(),
                        style = MaterialTheme.typography.subtitle1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

    }

    Spacer(modifier.weight(1f))
}


@Composable
fun BaseOnboardingHeaderContent(
    modifier: Modifier = Modifier,
    titleKey: Int,
    titleTag: String?,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        AnimatedContent(
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 20.dp)
                .zIndex(1f),
            targetState = titleKey,
            label = "animated info",
            transitionSpec = onboardingContentTransitionSpec(fixedTransitionHeight = 24)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = CoreAppIcons.LauncherLogo.asPainterResource(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(32.dp)
                        .alpha(animateFloatAsState(if (titleTag != null) 1f else 0f).value)
                        .background(
                            color = MaterialTheme.colors.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
                Text(
                    text = titleTag ?: "",
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        content()
    }
}

@Composable
inline fun <reified T : Comparable<T>> BaseOnboardingHeaderContent(
    modifier: Modifier = Modifier,
    titleKey: Int,
    animatedContentKey: T,
    titleTag: String?,
    crossinline content: @Composable (T) -> Unit,
) {
    BaseOnboardingHeaderContent(
        modifier = modifier,
        titleKey = titleKey,
        titleTag = titleTag,
        content = {
            AnimatedContent(
                modifier = Modifier.zIndex(0f),
                targetState = animatedContentKey,
                transitionSpec = onboardingContentTransitionSpec(fixedTransitionHeight = 100),
                label = "animated header"
            ) {
                content(it)
            }
        }
    )
}

inline fun <reified T : Comparable<T>> onboardingContentTransitionSpec(
    fixedTransitionHeight: Int? = null
): AnimatedContentTransitionScope<T>.() -> ContentTransform {
    return {
        if (targetState > initialState) {
            slideInVertically { height -> fixedTransitionHeight ?: height } + fadeIn() togetherWith
                    slideOutVertically { height -> -(fixedTransitionHeight ?: height) } + fadeOut()
        } else {
            slideInVertically { height ->
                -(fixedTransitionHeight ?: height)
            } + fadeIn() togetherWith
                    slideOutVertically { height -> fixedTransitionHeight ?: height } + fadeOut()
        }.using(
            // Disable clipping since the faded slide-in/out should
            // be displayed out of bounds.
            SizeTransform(clip = false)
        )
    }
}