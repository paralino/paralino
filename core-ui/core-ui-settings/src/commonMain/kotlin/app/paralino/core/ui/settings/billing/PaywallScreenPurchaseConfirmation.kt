package app.paralino.core.ui.settings.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.BaseOnboardingHeaderContent
import app.paralino.core.ui.components.BaseOnboardingScreen
import app.paralino.core.ui.components.BaseOnboardingScrollableContent
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PaywallScreenPurchaseConfirmation(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues
) {
    BaseOnboardingScreen(
        modifier = modifier,
        nextButton = null,
        skipButton = null,
        content = { rootPaddingValues ->
            BaseOnboardingScrollableContent(
                modifier = Modifier,
                order = 1,
                title = stringResource(MR.strings.paywall_purchase_confirmation_title),
                info = stringResource(MR.strings.paywall_purchase_confirmation_info),
                contentSizeRatio = 0.4f,
                minHeaderSize = 300.dp,
                paddingValues = rootPaddingValues,
                innerPaddingValues = paddingValues,
                headerContent = {
                    BaseOnboardingHeaderContent(
                        modifier = Modifier,
                        titleKey = 1,
                        titleTag = stringResource(MR.strings.paywall_screen_title),
                        content = {
                            PurchaseConfirmationHeader()
                        }
                    )
                }
            )
        }
    )
}

@Composable
private fun PurchaseConfirmationHeader() {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp, top = 12.dp)
            .fillMaxSize()
            .dropShadow(
                shape = RoundedCornerShape(20.dp),
                shadow = Shadow(
                    radius = 12.dp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                )
            )
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colors.surface)
            .padding(6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.accentDark.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = CoreAppIcons.StarFamily.asPainterResource(),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colors.onPrimary
            )
        }
    }
}
