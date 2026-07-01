package app.paralino.core.ui.places

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.create.CreatePlaceScreenUiEvent
import app.paralino.core.presentation.places.create.PlaceAddressSearchUiMode
import app.paralino.core.resources.MokoResources.imageAsPainterResource
import app.paralino.core.ui.components.EmbeddedSlider
import app.paralino.core.ui.components.EmbeddedSliderSize
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.LocalScreenDimensionType
import app.paralino.core.utils.extensions.conditional
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun CreatePlaceMapStep(
    modifier: Modifier = Modifier.Companion,
    focusManager: FocusManager,
    focusRequester: FocusRequester,
    state: CreatePlaceScreenState,
    onImageClick: () -> Unit,
    onEvent: (CreatePlaceScreenUiEvent) -> Unit,
    mapContent: @Composable (CreatePlaceScreenState) -> Unit,
) {
    val isSmallScreen = LocalScreenDimensionType.current.isSmall
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = state.addressUiMode == PlaceAddressSearchUiMode.HIDDEN && !isSmallScreen && !state.editOnlyCurrentStep
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(64.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                ImageModelImage(
                    imageModel = state.image,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { onImageClick() }
                        .align(Alignment.Bottom),
                )

                Spacer(modifier = Modifier.size(8.dp))

                MainInputField(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    text = state.name,
                    maxTextLength = CreatePlaceScreenState.placeNameLengthLimit,
                    hint = stringResource(MR.strings.places_create_name_placeholder),
                    focusManager = focusManager,
                    hasError = state.hasNameError,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    onInputChange = {
                        onEvent(CreatePlaceScreenUiEvent.OnNameChanged(it))
                    }
                )
            }
        }

        // map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (isSmallScreen) 12.dp else 20.dp
                )
                .weight(1f)
                .background(
                    color = MaterialTheme.colors.background,
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    border = BorderStroke(6.dp, MaterialTheme.colors.background),
                    shape = RoundedCornerShape(20.dp)
                )
                .dropShadow(
                    shape = RoundedCornerShape(20.dp),
                    shadow = Shadow(
                        radius = 12.dp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                    )
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colors.background)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colors.background,
                ) {
                    mapContent(state)
                }

                val sliderSize = remember {
                    EmbeddedSliderSize(minStepWidth = 120.dp)
                }
                AnimatedVisibility(visible = state.addressUiMode == PlaceAddressSearchUiMode.HIDDEN) {
                    EmbeddedSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .padding(bottom = 6.dp, top = 2.dp)
                            .height(sliderSize.minHeight),
                        title = stringResource(MR.strings.places_create_seekbar_info),
                        value = state.radiusText,
                        size = sliderSize,
                        progress = state.currentStep,
                        steps = CreatePlaceScreenState.radiusSteps.size,
                        onProgressChange = { index ->
                            onEvent(
                                CreatePlaceScreenUiEvent.OnRadiusChanged(
                                    CreatePlaceScreenState.radiusForIndex(index)
                                )
                            )
                        }
                    )
                }
            }

            if (state.hasSatelliteMode) {
                Image(
                    painter = if (!state.isSatelliteModeActive) {
                        MR.images.image_map_type_satellite.imageAsPainterResource()
                    } else if (isSystemInDarkTheme()) {
                        MR.images.image_map_type_default_dark.imageAsPainterResource()
                    } else {
                        MR.images.image_map_type_default_light.imageAsPainterResource()
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 16.dp, end = 16.dp, start = 16.dp)
                        .size(40.dp)
                        .clickable { onEvent(CreatePlaceScreenUiEvent.OnMapModeSwitch) }
                        .background(
                            color = AppColors.accentDark,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            PlaceAddressSearchComponent(
                modifier = Modifier
                    .fillMaxSize()
                    .conditional(state.addressUiMode == PlaceAddressSearchUiMode.FULLY_EXPANDED) {
                        clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                            onEvent(CreatePlaceScreenUiEvent.ChangeSearchFieldFocus(isFocused = false))
                        }
                    }
                    .padding(4.dp),
                focusManager = focusManager,
                focusRequester = focusRequester,
                uiMode = state.addressUiMode,
                isLoading = state.isAddressLoading,
                address = state.address,
                suggestions = state.addressSuggestions,
                onAddressChange = { onEvent(CreatePlaceScreenUiEvent.OnAddressChanged(it)) },
                onFocusToggle = { onEvent(CreatePlaceScreenUiEvent.ToggleSearchFieldFocus) },
                onAddressSuggestionSelected = { onEvent(CreatePlaceScreenUiEvent.OnAddressSuggestionSelected(it)) }
            )
        }

        if (!isSmallScreen) {
            Text(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.caption.copy(
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                ),
                text = state.hintText
            )

            Spacer(modifier = Modifier.size(12.dp))
        } else {
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}