package com.zbapp.locationtrack.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.zbapp.core.models.Payload
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.utils.ConditionErrorType
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.ui.components.DefaultEmptyState
import com.zbapp.locationtrack.ui.components.FilledButton
import com.zbapp.locationtrack.ui.components.ImageModelImage
import com.zbapp.locationtrack.ui.settings.SettingsSectionHeader
import com.zbapp.locationtrack.ui.shared.list.ErrorItems
import com.zbapp.locationtrack.ui.shared.list.ErrorListItemContainer
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import com.zbapp.locationtrack.utils.extensions.getActivity
import org.koin.androidx.compose.getViewModel

@Composable
fun DashboardScreen(
    navController: NavController,
    vm: DashboardViewModel = getViewModel()
) {

    val activity = LocalContext.current.getActivity()!!
    val permissionHandler = LocationPermissionHandler(
        activity.application as AppClass,
        activity,
        activity.activityResultRegistry
    )
    val vmState by vm.uiState.collectAsState(Payload.Loading)
    val state = vmState

    when (state) {
        is Payload.Error -> {
            DefaultEmptyState(content = stringResource(id = R.string.error_something_wrong))
        }
        Payload.Loading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        is Payload.Success -> {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = scrollState)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {

                if (state.data.errors.isNotEmpty()) {
                    AndroidView(
                        factory = {
                            ErrorListItemContainer(it).also {
                                it.itemClick = {
                                    when (it) {
                                        ConditionErrorType.GPS -> permissionHandler.askLocationService()
                                        ConditionErrorType.PERMISSION_LOCATION -> permissionHandler.askLocationPermission()
                                        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> permissionHandler.askBackgroundLocationPermission()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        update = {
                            it.itemData = ErrorItems(state.data.errors)
                        }
                    )
                }

                if (state.data.isWithoutGroups) {
                    DefaultEmptyState(
                        content = stringResource(id = R.string.dashboard_info_no_groups),
                        icon = Icons.Rounded.Groups,
                        buttonText = stringResource(id = R.string.dashboard_groups_add_button),
                        bigButton = true,
                        onClick = {
                            // TODO open group create
                        }
                    )
                } else {

                    if (state.data.activeGroups.isNotEmpty()) {
                        SettingsSectionHeader(name = stringResource(id = R.string.dashboard_groups_section_active))
                        state.data.activeGroups.forEach {
                            GroupListItem(data = it) {
                                // TODO open group details
                            }
                        }
                    }

                    if (state.data.inactiveGroups.isNotEmpty()) {
                        SettingsSectionHeader(name = stringResource(id = R.string.dashboard_groups_section_inactive))
                        state.data.inactiveGroups.forEach {
                            GroupListItem(data = it) {
                                // TODO open group details
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(100.dp))
                    FilledButton(
                        style = ButtonStyle.SMALL,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = {
                            // TODO open group create
                        }
                    ) {
                        Icon(Icons.Rounded.GroupAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(text = stringResource(id = R.string.dashboard_groups_add_button))
                    }

                }
            }
        }
    }

}

@Composable
fun GroupListItem(
    data: GroupItemData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .run {
                if (data.uiType != GroupItemData.GroupUiType.PICKER)
                    this.background(MaterialTheme.colors.surface)
                else this
            }
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageModelImage(
            imageModel = data.image,
            modifier = Modifier
                .size(50.dp)
                .border(
                    0.5.dp,
                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                    RoundedCornerShape(8.dp)
                )
        )

        Spacer(modifier = Modifier.size(16.dp))
        Column(
            modifier = Modifier
        ) {
            Text(
                text = data.name,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier
            )

            if (data.userCount != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            modifier = Modifier.size(16.dp),
                            contentDescription = null
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = "${data.userCount}",
                            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}
