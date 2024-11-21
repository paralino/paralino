package app.paralino.core.ui.places

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.ActionScaffold
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.map.MapMarkerContent
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.utils.extensions.pulsate
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

@Composable
fun PlacesEmptyState(
    modifier: Modifier = Modifier,
    permissionError: ConditionErrorType.PermissionNotifications?,
    onPostSampleNotification: (String) -> Unit,
    onErrorItemClick: (ConditionErrorType) -> Unit,
    onCreateClick: () -> Unit
) {
    ActionScaffold(
        modifier = modifier.fillMaxSize(),
        buttonText = stringResource(MR.strings.places_create_button_create),
        buttonIcon = CoreAppIcons.MapsHomeWork.asPainterResource(),
        onConfirm = onCreateClick,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(it)
                .padding(vertical = 20.dp)
        ) {
            var notificationVisibility by rememberSaveable { mutableStateOf(false) }
            val notificationContentEnter = remember {
                MR.strings.place_notification_enter.localizedString(
                    "Peter",
                    MR.strings.places_notification_demo_school.localizedString()
                )
            }
            val notificationContentExit = remember {
                MR.strings.place_notification_exit.localizedString(
                    "Heidi",
                    MR.strings.places_notification_demo_home.localizedString()
                )
            }

            LaunchedEffect(Unit) {
                delay(3000)
                notificationVisibility = true
            }

            AnimatedVisibility(visible = permissionError != null) {
                ConditionErrorItem(
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp),
                    data = ConditionErrorType.PermissionNotifications,
                    onClick = { onErrorItemClick(ConditionErrorType.PermissionNotifications) }
                )
            }

            AnimatedVisibility(
                visible = notificationVisibility
            ) {
                NotificationMockupItem(
                    modifier = Modifier
                        .rotate(2f)
                        .pulsate(pulseAmount = 0.95f),
                    description = notificationContentEnter,
                    onClick = { onPostSampleNotification(notificationContentEnter) }
                )
            }

            AnimatedVisibility(
                visible = notificationVisibility
            ) {
                NotificationMockupItem(
                    modifier = Modifier
                        .rotate(-2f)
                        .padding(top = 12.dp, bottom = 30.dp)
                        .pulsate(pulseAmount = 0.9f),
                    description = notificationContentExit,
                    onClick = { onPostSampleNotification(notificationContentExit) }
                )
            }

            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = !notificationVisibility
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 20.dp, bottom = 40.dp)
                        .requiredSize(10.dp)
                        .pulsate(pulseAmount = 1.8f)
                        .clip(CircleShape)
                        .background(
                            color = AppColors.accent,
                            shape = CircleShape
                        )
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = if (isSystemInDarkTheme()) {
                        CoreAppIcons.MapMockupDark.asPainterResource()
                    } else {
                        CoreAppIcons.MapMockupLight.asPainterResource()
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .border(
                            BorderStroke(6.dp, MaterialTheme.colors.surface),
                            RoundedCornerShape(20.dp)
                        )
                        .fillMaxSize()
                        .height(300.dp)
                        .shadow(elevation = 6.dp, RoundedCornerShape(20.dp))
                )

                val markers = remember {
                    PlacesEmptyStateMarkers()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize()
                        .padding(horizontal = 20.dp)
                        .padding(6.dp)
                        .clipToBounds()
                ) {
                    Box(
                        modifier = Modifier
                            .clipToBounds()
                            .align(Alignment.TopStart)
                    ) {
                        var markerPadding by rememberSaveable { mutableFloatStateOf(0f) }
                        LaunchedEffect(Unit) {
                            delay(1000)
                            markerPadding = 1f
                        }

                        Box(
                            modifier = Modifier.padding(top = 30.dp, start = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(markers.school.accuracy.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSystemInDarkTheme()) {
                                            AppColors.accent.copy(alpha = 0.15f)
                                        } else {
                                            Color.Blue.copy(alpha = 0.15f)
                                        }
                                    )
                            )
                            MapMarkerContent(state = markers.school)
                        }
                        MapMarkerContent(
                            modifier = Modifier
                                .offset(
                                    x = animateDpAsState(
                                        targetValue = markerPadding.times(160).minus(100).dp,
                                        animationSpec = tween(durationMillis = 1200)
                                    ).value,
                                    y = animateDpAsState(
                                        targetValue = markerPadding.times(-180).plus(200).dp,
                                        animationSpec = tween(durationMillis = 700)
                                    ).value
                                ),
                            state = markers.peter
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        var markerPadding by rememberSaveable { mutableFloatStateOf(0f) }
                        LaunchedEffect(Unit) {
                            delay(500)
                            markerPadding = 1f
                        }
                        Box(
                            modifier = Modifier,
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(markers.home.accuracy.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSystemInDarkTheme()) {
                                            AppColors.accent.copy(alpha = 0.15f)
                                        } else {
                                            Color.Blue.copy(alpha = 0.15f)
                                        }
                                    )
                            )
                            MapMarkerContent(state = markers.home)
                        }
                        MapMarkerContent(
                            modifier = Modifier
                                .offset(
                                    x = animateDpAsState(
                                        targetValue = markerPadding.times(-120).plus(-20).dp,
                                        animationSpec = tween(durationMillis = 1000)
                                    ).value,
                                    y = animateDpAsState(
                                        targetValue = markerPadding.times(40).plus(-40).dp,
                                        animationSpec = tween(durationMillis = 500)
                                    ).value
                                ),
                            state = markers.heidi
                        )
                    }
                }
            }

            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(
                    modifier = Modifier.padding(20.dp),
                    text = stringResource(MR.strings.places_list_screen_empty_info).annotatedBold(),
                    style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class PlacesEmptyStateMarkers(
    val peter: MapPinData = MapPinData(
        groupId = "",
        markerId = "peter",
        pinType = MapPinType.MEMBER,
        markerStyle = MapMarkerStyle.PIN,
        focused = false,
        active = true,
        obsolete = false,
        hidden = false,
        title = "Peter",
        image = ImageModel.emoji(Emojis.goat),
        lat = 0.0,
        lng = 0.0,
        accuracy = 10f,
        shouldShowApproximateCircle = false
    ),
    val heidi: MapPinData = MapPinData(
        groupId = "",
        markerId = "heidi",
        pinType = MapPinType.MEMBER,
        markerStyle = MapMarkerStyle.PIN,
        focused = false,
        active = true,
        obsolete = false,
        hidden = false,
        title = "Heidi",
        image = ImageModel.emoji(Emojis.cow),
        lat = 0.0,
        lng = 0.0,
        accuracy = 10f,
        shouldShowApproximateCircle = false
    ),
    val home: MapPinData = MapPinData(
        groupId = "",
        markerId = "home",
        pinType = MapPinType.PLACE,
        markerStyle = MapMarkerStyle.CIRCLE,
        focused = true,
        active = true,
        obsolete = false,
        hidden = false,
        title = "Home",
        image = ImageModel.emoji(Emojis.home),
        lat = 0.0,
        lng = 0.0,
        accuracy = 100f,
        shouldShowApproximateCircle = true
    ),
    val school: MapPinData = MapPinData(
        groupId = "",
        markerId = "school",
        pinType = MapPinType.PLACE,
        markerStyle = MapMarkerStyle.CIRCLE,
        focused = true,
        active = true,
        obsolete = false,
        hidden = false,
        title = "School",
        image = ImageModel.emoji(Emojis.schoolBooks),
        lat = 0.0,
        lng = 0.0,
        accuracy = 150f,
        shouldShowApproximateCircle = true
    )
)