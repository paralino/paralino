package app.paralino.core.ui.onboarding

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.OnboardingSkipButton
import app.paralino.core.ui.components.ScaffoldBottomBarButtons
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBold
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
        BaseOnboardingContent(
            order = order,
            title = title,
            info = info,
            contentSizeRatio = contentSizeRatio,
            paddingValues = it,
            headerContent = headerContent
        )
    }
}

@Composable
fun BaseOnboardingScreen(
    modifier: Modifier = Modifier,
    buttonText: String = stringResource(MR.strings.button_next),
    skipButtonText: String = stringResource(MR.strings.button_skip),
    onNext: () -> Unit,
    onSkip: (() -> Unit)?,
    content: @Composable (PaddingValues) -> Unit
) {
    BaseOnboardingScreen(
        modifier = modifier,
        nextButton = @Composable {
            FilledButton(
                modifier = Modifier,
                style = ButtonStyle.BIG,
                state = AppButtonState.CONTENT,
                icon = null,
                onClick = onNext
            ) {
                Text(text = buttonText)
            }
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
fun BaseOnboardingContent(
    modifier: Modifier = Modifier,
    order: Int,
    title: String,
    info: String?,
    contentSizeRatio: Float,
    paddingValues: PaddingValues,
    headerContent: @Composable () -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            headerContent()
        }

        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxHeight(contentSizeRatio),
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
                        text = info.annotatedBold(),
                        style = MaterialTheme.typography.subtitle1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
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