package app.paralino.core.ui.settings.billing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.ui.onboarding.BaseOnboardingHeaderContent
import app.paralino.core.ui.onboarding.BaseOnboardingScreen
import app.paralino.core.ui.onboarding.BaseOnboardingScrollableContent
import app.paralino.core.ui.onboarding.welcome.WelcomeOnboardingPageLocationHistory
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PaywallScreenFeatureLocationHistory(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    content: String
) {
    BaseOnboardingScreen(
        modifier = modifier,
        nextButton = null,
        skipButton = null,
        content = { rootPaddingValues ->
            BaseOnboardingScrollableContent(
                modifier = Modifier,
                order = 1,
                title = stringResource(MR.strings.paywall_header_feature_history_title),
                info = content,
                contentSizeRatio = 0.4f,
                minHeaderSize = 300.dp,
                paddingValues = rootPaddingValues,
                innerPaddingValues = paddingValues,
                headerContent = {
                    BaseOnboardingHeaderContent(
                        modifier = Modifier,
                        titleKey = 1,
                        titleTag = stringResource(MR.strings.personalSpace_spaceTitle),
                        content = {
                            WelcomeOnboardingPageLocationHistory()
                        }
                    )
                }
            )
        }
    )
}