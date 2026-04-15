package app.paralino.core.ui.onboarding.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.onboarding.AccountOnboardingContent
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.invite.manage.PersonalCodeItem
import app.paralino.core.ui.invite.manage.PersonalCodeItemWithHints
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AccountOnboardingParalinoID(
    modifier: Modifier = Modifier,
    step: AccountOnboardingContent.ParalinoID,
    onSharePersonalCode: (String) -> Unit,
    onCopyPersonalCode: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(top = 12.dp)
            .fillMaxSize()
    ) {
        val personalCode = step.personalCode
        if (personalCode != null) {
            PersonalCodeItemWithHints(hintDelayMillis = 800) {
                PersonalCodeItem(
                    modifier = this,
                    state = personalCode,
                    hintDelayMillis = 0,
                    hasShareHint = false,
                    onCopy = onCopyPersonalCode,
                    onShare = onSharePersonalCode,
                    onDelete = {}
                )
            }
        } else {
            DefaultEmptyState(
                modifier = Modifier,
                icon = CoreAppIcons.AddLink.asPainterResource(),
                content = stringResource(MR.strings.invite_manage_empty_description)
            )
        }
    }
}