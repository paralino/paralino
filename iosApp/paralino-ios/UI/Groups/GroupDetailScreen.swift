//
//  GroupDetailScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 07.07.23.
//

import SwiftUI
import shared

struct GroupDetailScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
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
                    openMemberAdd: {
                        path.append(.home(HomeGraph.memberInvite(groupId: appState.vm.groupId, code: nil, isOnboarding: false)))
                    },
                    openMap: {
                        path.removeAll()
                        path.append(.home(.map))
                    },
                    openPlaces: {
                        path.append(.home(.places(groupId: appState.vm.groupId, isOnboarding: false)))
                    },
                    openGroupSettings: {
                        path.append(.home(.groupSettings(groupId: appState.vm.groupId)))
                    },
                    openMemberRawData: { memberId in
                        path.append(.settings(.rawData(type: RawPresentationDataType.GroupMemberPresentation(memberId: memberId))))
                    },
                    openSharing: {
                        path.append(.home(.sharingStart(groupId: appState.vm.groupId, isOnboarding: false)))
                    },
                    openPaywall: {
                        
                    },
                    onNavigateBack: {
                        path.removeLast()
                    },
                    askNotificationPermission: {
                        appDelegate.notificationPermissionHandler.requestNotificationPermission(
                            messageDispather: appState.vm.messageDispatcher
                        )
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
