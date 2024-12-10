package app.paralino.android.ui.onboarding.welcome

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.places.PlaceEmptyScreenMapContent

@Composable
fun WelcomeOnboardingPage3(
    modifier: Modifier = Modifier
) {
    PlaceEmptyScreenMapContent(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        mapShadowElevation = 1.dp
    )
}
