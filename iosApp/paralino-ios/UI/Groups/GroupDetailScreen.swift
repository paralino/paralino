//
//  GroupDetailScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 07.07.23.
//

import SwiftUI
import shared

struct GroupDetailScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var appState: ViewModelFactoryIOS<GroupDetailViewModel>
    @State var alertDialog: AlertDialogData? = nil
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.GroupDetailScreenVC(
                    vm: appState.vm,
                    groupId: appState.vm.groupId,
                    onShowDialog: { dialog in
                        alertDialog = dialog != nil ? AlertDialogData(coreData: dialog!) : nil
                    },
                    openMemberAdd: {
                        path.append(.home(.memberInvite(groupId: appState.vm.groupId, code: nil)))
                    },
                    openGroupEdit: {
                        path.append(.home(.groupCreate(groupId: appState.vm.groupId)))
                    },
                    openMap: {
                        path.removeAll()
                        path.append(.home(.map))
                    },
                    openPlaces: {
                        
                    },
                    openGroupRawData: {
                        path.append(.settings(.rawData(type: RawPresentationDataType.GroupPresentation(groupId: appState.vm.groupId))))
                    },
                    openMemberRawData: { memberId in
                        path.append(.settings(.rawData(type: RawPresentationDataType.GroupMemberPresentation(memberId: memberId))))
                    },
                    openSharing: {
                        
                    },
                    onNavigateBack: {
                        path.removeLast()
                    },
                    askNotificationPermission: {
                        
                    },
                    askLocationService: {
                        
                    },
                    askLocationPermission: {
                        
                    },
                    askBackgroundLocationPermission: {
                        
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
        }
        .mainAlert($alertDialog) { key in
            if let dialogData = alertDialog?.coreDialog {
                if dialogData is MainAlertDialogGroupOptions {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                        switch key {
                        case "delete": alertDialog = AlertDialogData(coreData: MainAlertDialogDeleteGroup())
                        case "leave": alertDialog = AlertDialogData(coreData: MainAlertDialogLeaveGroup())
                        case "block": alertDialog = AlertDialogData(coreData: MainAlertDialogBlockGroup())
                        default: do {}
                        }
                    }
                } else if dialogData is MainAlertDialogDeleteGroup || dialogData is MainAlertDialogLeaveGroup {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.leaveOrDeleteGroup()
                    }
                } else if dialogData is MainAlertDialogBlockGroup {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.blockGroup()
                    }
                } else if let dialogData = dialogData as? MainAlertDialogBlockUser {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.blockUser(userId: dialogData.userId)
                    }
                } else if let dialogData = dialogData as? MainAlertDialogUnblockGroupOrUser {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.unblockUser(userId: dialogData.id)
                    }
                } else if let dialogData = dialogData as? MainAlertDialogDeleteGroupMember {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.removeMember(memberUserId: dialogData.memberUserId)
                    }
                } else if let dialogData = dialogData as? MainAlertDialogDeleteMemberLocation {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.removeMemberLocation(memberId: dialogData.memberId, locationId: dialogData.locationId)
                    }
                } else if dialogData is MainAlertDialogPlanLimit {
                    // do nothing
                }
            }
        }
    }
    
    static func createState(groupId: String) -> ViewModelFactoryIOS<GroupDetailViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.groupDetailVM(groupId: groupId))
    }
}

struct GroupDetailScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.home(.groupDetail(groupId: ""))]
    static var previews: some View {
        GroupDetailScreen(path: $path, appState: GroupDetailScreen.createState(groupId: ""))
    }
}
