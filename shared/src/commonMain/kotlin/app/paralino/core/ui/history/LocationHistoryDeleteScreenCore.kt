package app.paralino.core.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.DeviceType
import app.paralino.core.presentation.history.LocationHistoryDeleteDateState
import app.paralino.core.presentation.history.LocationHistoryDeleteScreenEvent
import app.paralino.core.presentation.history.LocationHistoryDeleteUiEvent
import app.paralino.core.presentation.history.LocationHistoryDeleteViewModel
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.provider.current
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CheckButtonIcon
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.ScaffoldBottomBarButton
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.LocalDate


@Composable
fun LocationHistoryDeleteScreenCore(
    appState: CoreUiState,
    vm: LocationHistoryDeleteViewModel,
    onNavigateBack: () -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                LocationHistoryDeleteScreenEvent.NavigateBack -> {
                    onNavigateBack()
                }

                null -> {}
            }
        }
    }

    uiState.alertDialog?.let { dialog ->
        dialog.Show {
            vm.onEvent(LocationHistoryDeleteUiEvent.DialogAction(AlertDialogAction(dialog, it)))
        }
    }

    if (!uiState.isContentLoading) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                AnimatedVisibility(
                    visible = uiState.isDeleteButtonEnabled,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = slideOutVertically { it / 2 } + fadeOut(),
                ) {
                    ScaffoldBottomBarButton(
                        buttonState = AppButtonState.loadingStateWithCheck(uiState.isDeleteLoading),
                        buttonText = stringResource(MR.strings.button_delete),
                        buttonIcon = null,
                        onClick = { vm.onEvent(LocationHistoryDeleteUiEvent.OnDeleteAction) }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.Top
            ) {
                DataInfoChip(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    image = uiState.groupImage,
                    text = uiState.groupName,
                    backgroundColor = if (uiState.isPersonalSpace) {
                        AppColors.accent.copy(0.1f)
                    } else {
                        MaterialTheme.colors.surface
                    }
                )

                if (uiState.isDeletingEverything) {
                    NoticeInfoChip(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .padding(horizontal = 20.dp),
                        text = stringResource(MR.strings.history_delete_allDevices_notice),
                        icon = CoreAppIcons.Warning.asPainterResource(),
                        backgroundColor = AppColors.errorBackground,
                        textColor = AppColors.redMain,
                        iconTint = AppColors.redMain
                    )
                } else if (!uiState.isContentLoading) {
                    SettingsSectionHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        name = stringResource(MR.strings.group_member_devices_section_title),
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        this.items(uiState.devices, key = { it.locationId }) { item ->
                            DataInfoChip(
                                modifier = Modifier,
                                innerPaddingValues = PaddingValues(
                                    top = 5.dp,
                                    bottom = 5.dp,
                                    start = 6.dp,
                                    end = 8.dp
                                ),
                                imageOrIcon = {
                                    CheckButtonIcon(
                                        isChecked = item.isSelected,
                                        size = 18.dp
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                },
                                text = {
                                    Column(
                                        modifier = Modifier
                                            .offset(y = if (item.tag.isNullOrBlank()) 0.dp else (-2).dp)
                                    ) {
                                        Text(
                                            modifier = Modifier,
                                            text = item.deviceName,
                                            style = MaterialTheme.typography.body2,
                                        )
                                        if (!item.tag.isNullOrBlank()) {
                                            Text(
                                                text = item.tag,
                                                style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colors.onSecondary,
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colors.secondary,
                                                        shape = MaterialTheme.shapes.medium
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 0.dp)
                                            )
                                        }
                                    }
                                },
                                endSection = null,
                                onClick = {
                                    vm.onEvent(LocationHistoryDeleteUiEvent.ToggleDeviceSelection(item))
                                }
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = uiState.hasDeviceSelectionError
                    ) {
                        Text(
                            text = stringResource(MR.strings.history_delete_devices_noSelectionError),
                            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colors.error,
                            modifier = Modifier
                                .padding(horizontal = 28.dp)
                                .padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(10.dp))

                SettingsSectionHeader(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    name = stringResource(MR.strings.history_delete_section_period_title),
                    count = null,
                    limit = null
                )

                DateSelectionOptionsContainer(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fromDate = uiState.fromDate,
                    untilDate = uiState.untilDate,
                    onEvent = vm::onEvent
                )

                Spacer(modifier = Modifier.size(20.dp))

                AnimatedVisibility(uiState.deleteInfoContent != null) {
                    Text(
                        text = (uiState.deleteInfoContent ?: "").annotatedBoldCompose(
                            textColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        ),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    )
                }

                Spacer(modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DateSelectionOptionsContainer(
    modifier: Modifier = Modifier,
    fromDate: LocationHistoryDeleteDateState,
    untilDate: LocationHistoryDeleteDateState,
    onEvent: (LocationHistoryDeleteUiEvent) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color = MaterialTheme.colors.surface)
    ) {
        Column {
            // From date option
            DateOptionItem(
                title = stringResource(MR.strings.history_delete_from_period_title),
                isEnabled = fromDate.isEnabled,
                onToggle = { onEvent(LocationHistoryDeleteUiEvent.ToggleStartDate) }
            )

            AnimatedVisibility(
                visible = fromDate.isEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                DatePickerSection(
                    dateState = fromDate,
                    onNavigateYear = {
                        if (it) {
                            onEvent(LocationHistoryDeleteUiEvent.IncreaseStartYear)
                        } else {
                            onEvent(LocationHistoryDeleteUiEvent.DecreaseStartYear)
                        }
                    },
                    onNavigateMonth = {
                        if (it) {
                            onEvent(LocationHistoryDeleteUiEvent.IncreaseStartMonth)
                        } else {
                            onEvent(LocationHistoryDeleteUiEvent.DecreaseStartMonth)
                        }
                    },
                    onSelectDay = {
                        onEvent(LocationHistoryDeleteUiEvent.OnSelectStartDay(it))
                    }
                )
            }
        }

        Divider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = Color.Gray.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )

        Column {
            // Until date option
            DateOptionItem(
                title = stringResource(MR.strings.history_delete_until_period_title),
                isEnabled = untilDate.isEnabled,
                onToggle = { onEvent(LocationHistoryDeleteUiEvent.ToggleEndDate) }
            )

            AnimatedVisibility(
                visible = untilDate.isEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                DatePickerSection(
                    dateState = untilDate,
                    onNavigateYear = {
                        if (it) {
                            onEvent(LocationHistoryDeleteUiEvent.IncreaseEndYear)
                        } else {
                            onEvent(LocationHistoryDeleteUiEvent.DecreaseEndYear)
                        }
                    },
                    onNavigateMonth = {
                        if (it) {
                            onEvent(LocationHistoryDeleteUiEvent.IncreaseEndMonth)
                        } else {
                            onEvent(LocationHistoryDeleteUiEvent.DecreaseEndMonth)
                        }
                    },
                    onSelectDay = {
                        onEvent(LocationHistoryDeleteUiEvent.OnSelectEndDay(it))
                    }
                )
            }
        }
    }
}

@Composable
private fun DateOptionItem(
    title: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckButtonIcon(
            modifier = Modifier.align(Alignment.CenterVertically),
            isChecked = isEnabled
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1.copy(
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                lineHeight = if (DeviceType.current == DeviceType.iOS) 18.sp else 24.sp,
            )
        )
    }
}

@Composable
private fun DatePickerSection(
    dateState: LocationHistoryDeleteDateState,
    onNavigateYear: (Boolean) -> Unit,
    onNavigateMonth: (Boolean) -> Unit,
    onSelectDay: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Year and Month selectors in a Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Year selector
            YearMonthSelector(
                modifier = Modifier.weight(1f),
                value = dateState.year.toString(),
                onNavigateBack = { onNavigateYear(false) },
                onNavigateForward = { onNavigateYear(true) }
            )

            // Month selector
            YearMonthSelector(
                modifier = Modifier.weight(1.3f),
                value = dateState.formattedMonth,
                onNavigateBack = { onNavigateMonth(false) },
                onNavigateForward = { onNavigateMonth(true) }
            )
        }

        // Date selector using existing LocationHistoryDateSelector
        LocationHistoryDateSelector(
            modifier = Modifier.fillMaxWidth(),
            state = dateState.selectorState,
            backgroundColor = MaterialTheme.colors.background,
            elevation = 0.dp,
            cornerRadiusDp = 12.dp,
            onEvent = { event ->
                when (event) {
                    is LocationHistoryUiEvent.OnSelectDay -> onSelectDay(event.day)
                    else -> {} // Ignore other events for now
                }
            }
        )
    }
}

@Composable
private fun YearMonthSelector(
    modifier: Modifier = Modifier,
    value: String,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.background)
//            .padding(horizontal = 12.dp, vertical = 8.dp)
        ,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onNavigateBack() }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = CoreAppIcons.ChevronLeft.asPainterResource(),
                contentDescription = "Previous",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colors.primary
            )
        }

        Text(
            modifier = Modifier.weight(1f),
            text = value,
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box(
            modifier = Modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onNavigateForward() }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = CoreAppIcons.ChevronRight.asPainterResource(),
                contentDescription = "Next",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colors.primary
            )
        }
    }
}