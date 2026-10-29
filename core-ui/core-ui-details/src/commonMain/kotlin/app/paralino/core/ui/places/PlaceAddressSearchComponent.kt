package app.paralino.core.ui.places

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.places.create.PlaceAddressSearchUiMode
import app.paralino.core.presentation.places.create.PlaceAddressSuggestion
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MainInputBasicField
import app.paralino.core.utils.extensions.conditional
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PlaceAddressSearchComponent(
    modifier: Modifier,
    focusManager: FocusManager,
    focusRequester: FocusRequester,
    uiMode: PlaceAddressSearchUiMode,
    isLoading: Boolean,
    address: TextFieldValue,
    suggestions: List<PlaceAddressSuggestion>,
    onAddressChange: (TextFieldValue) -> Unit,
    onAddressSuggestionSelected: (PlaceAddressSuggestion) -> Unit,
    onFocusToggle: () -> Unit,
    addressHint: String? = null,
) {
    Box(
        modifier = modifier
            .background(
                color = animateColorAsState(
                    if (uiMode == PlaceAddressSearchUiMode.FULLY_EXPANDED) {
                        MaterialTheme.colors.background
                    } else {
                        Color.Transparent
                    }
                ).value,
                shape = RoundedCornerShape(15.dp)
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = animateColorAsState(
                            if (uiMode != PlaceAddressSearchUiMode.HIDDEN) {
                                MaterialTheme.colors.background
                            } else {
                                Color.Transparent
                            }
                        ).value,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top
            ) {
                AnimatedVisibility(
                    modifier = Modifier.weight(1f),
                    visible = uiMode != PlaceAddressSearchUiMode.HIDDEN
                ) {
                    MainInputBasicField(
                        modifier = Modifier
                            .padding(start = 4.dp, end = 8.dp)
                            .padding(bottom = 8.dp, top = 4.dp)
                            .height(height = 48.dp)
                            .focusRequester(focusRequester),
                        textValue = address,
                        hint = addressHint ?: stringResource(MR.strings.places_create_address_placeholder),
                        focusManager = focusManager,
                        hasError = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Words,
                        trailingIcon = if (address.text.isNotEmpty()) {
                            {
                                Icon(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            onAddressChange(TextFieldValue(text = ""))
                                        }
                                        .clip(shape = CircleShape)
                                        .background(
                                            color = MaterialTheme.colors.primary,
                                            shape = CircleShape
                                        )
                                        .padding(4.dp),
                                    painter = CoreAppIcons.Close.asPainterResource(),
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.onPrimary
                                )
                            }
                        } else {
                            null
                        },
                        onInputChange = onAddressChange
                    )
                }

                val buttonColor = if (uiMode == PlaceAddressSearchUiMode.HIDDEN) {
                    MaterialTheme.colors.background
                } else {
                    MaterialTheme.colors.surface
                }
                val buttonShape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .padding(
                            top = animateDpAsState(if (uiMode == PlaceAddressSearchUiMode.HIDDEN) 8.dp else 4.dp).value,
                            bottom = 8.dp,
                            end = animateDpAsState(if (uiMode == PlaceAddressSearchUiMode.HIDDEN) 8.dp else 4.dp).value
                        )
                        .size(48.dp)
                        .clickable(onClick = onFocusToggle)
                        .conditional(uiMode == PlaceAddressSearchUiMode.HIDDEN) {
                            dropShadow(
                                shape = buttonShape,
                                shadow = Shadow(
                                    radius = 16.dp,
                                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                                )
                            )
                        }
                        .clip(shape = buttonShape)
                        .background(
                            color = animateColorAsState(buttonColor).value,
                            shape = buttonShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = contentColorFor(buttonColor)
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Icon(
                            modifier = Modifier.Companion,
                            painter = if (uiMode == PlaceAddressSearchUiMode.HIDDEN) {
                                CoreAppIcons.Search.asPainterResource()
                            } else {
                                CoreAppIcons.Close.asPainterResource()
                            },
                            contentDescription = null,
                            tint = contentColorFor(buttonColor)
                        )
                    }
                }
            }

            AnimatedVisibility(uiMode == PlaceAddressSearchUiMode.FULLY_EXPANDED) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    this.items(suggestions) { suggestion ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { onAddressSuggestionSelected(suggestion) },
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (suggestion.title.isNotBlank()) {
                                Text(
                                    text = suggestion.title,
                                    style = MaterialTheme.typography.subtitle2
                                )
                            }

                            if (suggestion.info.isNotBlank()) {
                                Text(
                                    text = suggestion.info,
                                    style = MaterialTheme.typography.caption
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}