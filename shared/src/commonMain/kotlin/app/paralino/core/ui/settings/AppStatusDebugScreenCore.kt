package app.paralino.core.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.presentation.settings.appstatus.AppStatusDebugScreenState
import app.paralino.core.presentation.settings.appstatus.AppStatusDebugScreenUiEvent
import app.paralino.core.presentation.settings.appstatus.AppStatusDebugViewModel
import app.paralino.core.provider.LogPriority
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBold

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterialApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun AppStatusDebugScreenCore(
    appState: CoreUiState,
    vm: AppStatusDebugViewModel,
    paddingValues: PaddingValues
) {

    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState(AppStatusDebugScreenState())

    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            },
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = paddingValues.calculateBottomPadding() + 20.dp
        ),
    ) {
        this.stickyHeader {
            Row(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .background(MaterialTheme.colors.background),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsSectionHeader(name = "Info")
            }
        }
        this.items(items = uiState.status, key = { it.key }) {
            Text(
                text = it.text.annotatedBold(),
                color = if (it.isError) MaterialTheme.colors.error else MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.caption.copy(lineHeight = 12.sp)
            )
        }
        this.item (key = "section_spacer") {
            Spacer(modifier = Modifier.size(8.dp))
        }
        this.stickyHeader {
            Column(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .background(MaterialTheme.colors.background)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSectionHeader(name = "Logs (${uiState.logs.size})")
                    Spacer(modifier = Modifier.weight(1f))
                    SmallActionButton(
                        modifier = Modifier,
                        icon = CoreAppIcons.Delete.asPainterResource(),
                        text = {
                            Text(
                                modifier = Modifier.padding(end = 4.dp),
                                text = "Clear"
                            )
                        },
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = MaterialTheme.colors.onPrimary,
                        onClick = { vm.onEvent(AppStatusDebugScreenUiEvent.ClearLogs) }
                    )
                }
                MainInputField(
                    modifier = Modifier.fillMaxWidth(),
                    text = uiState.query,
                    hint = "Search",
                    hasError = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                    focusManager = focusManager,
                    onInputChange = {
                        vm.onEvent(AppStatusDebugScreenUiEvent.SearchLogs(it))
                    }
                )
                Spacer(modifier = Modifier.size(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    uiState.filters.logPriorityFilters.forEach {
                        DataInfoChip(
                            image = null,
                            text = it.name,
                            backgroundColor = if (it.isOn) {
                                AppColors.accentLight
                            } else {
                                MaterialTheme.colors.surface
                            },
                            onClick = {
                                vm.onEvent(AppStatusDebugScreenUiEvent.ToggleLogPriorityFilter(it))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.size(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(uiState.filters.logTypeFilters, key = { it.key }) {
                        DataInfoChip(
                            image = null,
                            text = it.name,
                            backgroundColor = if (it.isOn) {
                                AppColors.accentLight
                            } else {
                                MaterialTheme.colors.surface
                            },
                            onClick = {
                                vm.onEvent(AppStatusDebugScreenUiEvent.ToggleLogTypeFilter(it))
                            }
                        )
                    }
                    items(uiState.filters.logHostsFilters, key = { it.key }) {
                        DataInfoChip(
                            image = null,
                            text = it.name,
                            backgroundColor = if (it.isOn) {
                                AppColors.accentLight
                            } else {
                                MaterialTheme.colors.surface
                            },
                            onClick = {
                                vm.onEvent(AppStatusDebugScreenUiEvent.ToggleAppHostTypeFilter(it))
                            }
                        )
                    }
                }
            }
        }
        this.items(items = uiState.logs, key = { it.key }) {
            Text(
                text = it.text,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = when (it.priority) {
                    LogPriority.WARN -> FontWeight.SemiBold
                    LogPriority.ERROR -> FontWeight.Bold
                    else -> FontWeight.Normal
                },
                color = when (it.priority) {
                    LogPriority.ERROR -> AppColors.redMain
                    else -> MaterialTheme.colors.onBackground
                }
            )
        }
    }
}