package app.paralino.core.ui.places

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MapMarkerContent
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBoldCompose
import app.paralino.core.utils.extensions.pulsate
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlacesEmptyState(
    modifier: Modifier = Modifier,
    isPersonalSpace: Boolean,
    permissionError: ConditionErrorType.PermissionNotifications?,
    onPostSampleNotification: (String) -> Unit,
    onErrorItemClick: (ConditionErrorType) -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {

        var notificationVisibility by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(3.seconds)
            notificationVisibility = true
        }

        PlaceEmptyStateNotificationItems(
            isPersonalSpace = isPersonalSpace,
            notificationVisibility = notificationVisibility,
            permissionError = permissionError,
            onPostSampleNotification = onPostSampleNotification,
            onErrorItemClick = onErrorItemClick
        )

        PlaceEmptyScreenMapContent(
            modifier = Modifier.padding(horizontal = 20.dp),
            isPersonalSpace = isPersonalSpace
        )

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                modifier = Modifier.padding(20.dp),
                text = if (isPersonalSpace) {
                    stringResource(MR.strings.places_list_screen_empty_info_personal).annotatedBoldCompose()
                } else {
                    stringResource(MR.strings.places_list_screen_empty_info_group).annotatedBoldCompose()
                },
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center
            )
        }

        NoticeInfoChip(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(vertical = 20.dp),
            text = if (isPersonalSpace) {
                stringResource(MR.strings.places_list_screen_encryption_info_personal
                )
            } else {
                stringResource(MR.strings.places_list_screen_encryption_info_group)
            }
        )
    }
}

@Composable
fun PlaceEmptyScreenMapContent(
    modifier: Modifier = Modifier,
    mapShadowElevation: Dp = 6.dp,
    isPersonalSpace: Boolean
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .height(300.dp)
            .shadow(mapShadowElevation, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Image(
            painter = if (isSystemInDarkTheme()) {
                CoreAppIcons.MapMockupDark.asPainterResource()
            } else {
                CoreAppIcons.MapMockupLight.asPainterResource()
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        val markers = remember(isPersonalSpace) {
            val markers = PlacesEmptyStateMarkers()
            if (isPersonalSpace) {
                markers.copy(
                    peter = markers.peter.copy(
                        image = ImageModel.emoji(Emojis.mobilePhone)
                    ),
                    heidi = markers.heidi.copy(
                        image = ImageModel.emoji(Emojis.mobilePhone)
                    )
                )
            } else {
                markers
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
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
}

@Composable
fun ColumnScope.PlaceEmptyStateNotificationItems(
    isPersonalSpace: Boolean,
    notificationVisibility: Boolean,
    permissionError: ConditionErrorType.PermissionNotifications?,
    onPostSampleNotification: (String) -> Unit,
    onErrorItemClick: (ConditionErrorType) -> Unit,
) {
    val notificationContentEnter = remember {
        MR.strings.place_notification_enter.localizedString(
            if (isPersonalSpace) {
                MR.strings.places_notification_personalDevice_placeholder.localizedString()
            } else {
                "Peter"
            },
            MR.strings.places_notification_demo_school.localizedString()
        )
    }
    val notificationContentExit = remember {
        MR.strings.place_notification_exit.localizedString(
            if (isPersonalSpace) {
                MR.strings.places_notification_personalDevice_placeholder.localizedString()
            } else {
                "Heidi"
            },
            MR.strings.places_notification_demo_home.localizedString()
        )
    }

    AnimatedVisibility(visible = permissionError != null) {
        ConditionErrorItem(
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp),
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
                .padding(top = 30.dp)
                .padding(horizontal = 12.dp)
                .pulsate(pulseAmount = 0.95f)
                .clickable { onPostSampleNotification(notificationContentEnter) },
            description = notificationContentEnter
        )
    }

    AnimatedVisibility(
        visible = notificationVisibility
    ) {
        NotificationMockupItem(
            modifier = Modifier
                .rotate(-2f)
                .padding(top = 12.dp, bottom = 30.dp)
                .padding(horizontal = 12.dp)
                .pulsate(pulseAmount = 0.9f)
                .clickable { onPostSampleNotification(notificationContentExit) }
            ,
            description = notificationContentExit
        )
    }

    AnimatedVisibility(
        modifier = Modifier.fillMaxWidth(),
        visible = !notificationVisibility
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 40.dp, bottom = 40.dp)
                .requiredSize(10.dp)
                .pulsate(pulseAmount = 1.8f)
                .clip(CircleShape)
                .background(
                    color = AppColors.accent,
                    shape = CircleShape
                )
        )
    }
}

data class PlacesEmptyStateMarkers(
    val peter: MapPinData = MapPinData(
        groupOrSpaceId = "".asGroupOrSpaceId(),
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
        groupOrSpaceId = "".asGroupOrSpaceId(),
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
        groupOrSpaceId = "".asGroupOrSpaceId(),
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
        groupOrSpaceId = "".asGroupOrSpaceId(),
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