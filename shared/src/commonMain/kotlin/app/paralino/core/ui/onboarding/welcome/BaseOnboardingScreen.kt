package app.paralino.core.ui.onboarding.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.FilledButton
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
    onNext: () -> Unit,
    onSkip: (() -> Unit)?,
    headerContent: @Composable () -> Unit
) {
    BaseOnboardingScreen(
        modifier = modifier,
        buttonText = buttonText,
        onNext = onNext,
        onSkip = onSkip
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(it),
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
                    .fillMaxHeight(0.3f),
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
}

@Composable
fun BaseOnboardingScreen(
    modifier: Modifier = Modifier,
    buttonText: String = stringResource(MR.strings.button_next),
    onNext: () -> Unit,
    onSkip: (() -> Unit)?,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            ScaffoldBottomBarButtons {
                FilledButton(
                    modifier = Modifier,
                    style = ButtonStyle.BIG,
                    state = AppButtonState.CONTENT,
                    icon = null,
                    onClick = onNext
                ) {
                    Text(text = buttonText)
                }
                if (onSkip != null) {
                    Spacer(Modifier.size(8.dp))
                    FilledButton(
                        modifier = Modifier,
                        style = ButtonStyle.BIG,
                        state = AppButtonState.CONTENT,
                        icon = null,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = MaterialTheme.colors.onBackground
                        ),
                        elevation = null,
                        onClick = onSkip
                    ) {
                        Text(text = stringResource(MR.strings.button_skip))
                    }
                }
            }
        },
        content = content
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
            slideInVertically { height -> -(fixedTransitionHeight ?: height) } + fadeIn() togetherWith
                    slideOutVertically { height -> fixedTransitionHeight ?: height } + fadeOut()
        }.using(
            // Disable clipping since the faded slide-in/out should
            // be displayed out of bounds.
            SizeTransform(clip = false)
        )
    }
}