package app.paralino.core.presentation.groups

sealed interface GroupMemberItemEvent {
    data class onItemClick(val item: GroupMemberItemState): GroupMemberItemEvent
    data class onActionClick(val action: MemberActionItemState, val item: GroupMemberItemState): GroupMemberItemEvent
    data class onLocationRequest(val item: GroupMemberItemState): GroupMemberItemEvent
    data class onDeleteDeviceClick(val item: GroupMemberItemLocationState): GroupMemberItemEvent
    data class onShowLocation(val item: GroupMemberItemStateWithLocations): GroupMemberItemEvent
}