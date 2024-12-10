package app.paralino.android.ui.onboarding.welcome

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.groups.dummyLocations
import app.paralino.android.ui.groups.dummyMemberState
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.MemberActionItemState
import app.paralino.core.presentation.groups.MemberActionType
import app.paralino.core.presentation.groups.MemberActionsState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.map.MapMarkerContent
import app.paralino.core.ui.places.PlacesEmptyStateMarkers
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.Emojis
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun WelcomeOnboardingPage1(
    modifier: Modifier = Modifier,
    isEncryptionOn: Boolean
) {
    Box(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        MapContent(modifier = Modifier.fillMaxSize())
        MemberItem(modifier = Modifier.align(Alignment.BottomCenter))
        EncryptedOverlay(isEncryptionOn = isEncryptionOn)
    }
}

@Composable
private fun MapContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Image(
            painter = CoreAppIcons.MapMockupSatelliteLawn.asPainterResource(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .height(300.dp)
        )

        val markers = remember {
            PlacesEmptyStateMarkers()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .clipToBounds()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .align(Alignment.TopStart)
            ) {
                var markerPadding by rememberSaveable { mutableFloatStateOf(0f) }
                LaunchedEffect(Unit) {
                    delay(10)
                    markerPadding = 1f
                }

                MapMarkerContent(
                    modifier = Modifier
                        .offset(
                            x = animateDpAsState(
                                targetValue = markerPadding.times(maxWidth.value).minus(100).dp,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 5000),
                                    repeatMode = RepeatMode.Reverse,
                                )
                            ).value,
                            y = animateDpAsState(
                                targetValue = markerPadding.times(-maxHeight.value / 3)
                                    .plus(200).dp,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 2500),
                                    repeatMode = RepeatMode.Reverse,
                                )
                            ).value
                        ),
                    state = markers.peter
                )
            }
        }
    }
}

@Composable
private fun MemberItem(modifier: Modifier = Modifier) {
    GroupMember(
        modifier = modifier
            .padding(12.dp),
        state = GroupMemberItemStateWithLocations(
            memberState = dummyMemberState.copy(
                name = "Peter", image = ImageModel.emoji(
                    Emojis.goat
                )
            ),
            locations = listOf(dummyLocations.get(1)),
            allowShowingDevices = false,
            actions = MemberActionsState(
                deleteMember = MemberActionItemState(
                    MemberActionType.DELETE_MEMBER
                )
            ),
            lastLocationPing = "16:30",
            lastStatusPing = "16:30",
        )
    ) { }
}

@Composable
private fun EncryptedOverlay(
    modifier: Modifier = Modifier,
    isEncryptionOn: Boolean
) {

    var text by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val seed = NanoId.generate(50)
        while (coroutineContext.isActive) {
            text = buildString {
                repeat(40) {
                    append(seed)
                }
            }.toList().shuffled().joinToString("")
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(
                TiltedRectangleShape(
                    percentage = animateFloatAsState(
                        if (isEncryptionOn) 1f else 0f,
                        tween(1000)
                    ).value
                )
            )
            .background(AppColors.accentDark)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colors.onPrimary,
            style = MaterialTheme.typography.overline
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            GroupMember(
                state = GroupMemberItemStateWithLocations(
                    memberState = dummyMemberState,
                    locations = listOf(dummyLocations.get(1)),
                    allowShowingDevices = false,
                    actions = MemberActionsState(
                        deleteMember = MemberActionItemState(
                            MemberActionType.DELETE_MEMBER
                        )
                    ),
                    lastLocationPing = "16:30",
                    lastStatusPing = "16:30",
                )
            ) { }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.accentLight)
                    .padding(8.dp)
            ) {
                Text(
                    text = text,
                    color = AppColors.darkBackground,
                    style = MaterialTheme.typography.overline
                )
            }
        }
    }
}

@Stable
private class TiltedRectangleShape(val percentage: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline.Generic {
        val path = Path().apply {
            lineTo(size.width * 0.7f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(size.width * 0.3f, size.height)
            lineTo(size.width * 0.7f, 0f)
            close()
        }

        path.translate(Offset(x = size.width * 0.72f * (1f - percentage), 0f))

        return Outline.Generic(path)
    }

    override fun toString(): String = "TiltedRectangleShape"
}
