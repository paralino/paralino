package app.paralino.android.ui.onboarding.welcome

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.places.PlaceEmptyScreenMapContent
import app.paralino.core.ui.places.PlaceEmptyStateNotificationItems
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun WelcomeOnboardingPage2(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        var notificationVisibility by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(2.seconds)
            notificationVisibility = true
        }

        PlaceEmptyScreenMapContent(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            mapShadowElevation = 1.dp
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colors.secondary.copy(
                                alpha = animateFloatAsState(
                                    if (notificationVisibility) 0.8f else 0f
                                ).value
                            ),
                            MaterialTheme.colors.secondary.copy(
                                alpha = animateFloatAsState(
                                    if (notificationVisibility) 0.8f else 0f
                                ).value
                            ),
                            Color.Transparent,
                        )
                    )
                ),
            verticalArrangement = Arrangement.Center
        ) {
            PlaceEmptyStateNotificationItems(
                showNotificationsAfter = 2.seconds,
                permissionError = null,
                onPostSampleNotification = {},
                onErrorItemClick = {}
            )
        }
    }
}
