package app.paralino.core.ui.onboarding.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.privacyzones.PrivacyZonesEmptyStateMapContent

@Composable
fun AccountOnboardingPrivacyZones(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp, top = 12.dp)
    ){
        PrivacyZonesEmptyStateMapContent(modifier = Modifier)
    }
}
