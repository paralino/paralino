//
//  SharingStartScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2023-11-24.
//

import SwiftUI
import shared
import paralino_apple_core
import SwiftLocation

struct SharingStartScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var appState: SharingStartState
    
    @State var groupPicker: BottomSheetPickerTypeGroupPickerBottomSheet? = nil
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.StartSharingScreenVC(
                    vm: appState.vm,
                    groupId: nil,
                    onPickGroup: { sheet in groupPicker = sheet },
                    onSharingStarted: {
                        finishWithSharingScreen(isOnboarding: appState.isOnboarding)
                    },
                    openSubscriptions: {
                        path.append(.settings(.subscriptions))
                    },
                    askNotificationPermission: {
                        appDelegate.notificationPermissionHandler.requestNotificationPermission(
                            messageDispather: appState.vm.messageDispatcher
                        )
                    },
                    askLocationService: {
                        appDelegate.locationPermissionHandler.requestLocationServices(
                            messageDispather: appState.vm.messageDispatcher
                        )
                    },
                    askLocationPermission: {
                        appDelegate.locationPermissionHandler.requestLocationPermission(
                            messageDispather: appState.vm.messageDispatcher
                        )
                    },
                    askBackgroundLocationPermission: {
                        appDelegate.locationPermissionHandler.requestLocationPermission(
                            messageDispather: appState.vm.messageDispatcher
                        )
                    }
                )
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .navigationTitle(MR.shared.strings.groups_sharing_title.localizedString())
        }
        .toolbar {
            if appState.isOnboarding {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        finishWithSharingScreen(isOnboarding: appState.isOnboarding)
                    }) {
                        Text(MR.shared.strings.button_skip.localizedString())
                    }
                }
            }
        }
        .groupPickerSheet(
            $groupPicker,
            onSelectGroup: { state in
                groupPicker = nil
                appState.vm.setSelectedGroup(groupId: state.id)
            },
            onEditGroup: { group in },
            onCreateGroup: {},
            onJoinGroup: {}
        )
    }
    
    private func finishWithSharingScreen(isOnboarding: Bool) {
        if appState.vm.shouldProceedToPaywall {
            if let index = path.firstIndex(where: { $0 == .home(.map) }) {
                path.removeSubrange(index+1..<path.count)
            }
            path.append(
                .settings(
                    .paywall(groupId: appState.groupId, type: isOnboarding ? BillingScreenSourceType.onboarding : BillingScreenSourceType.normal)
                )
            )
        } else if isOnboarding {
            if let index = path.firstIndex(where: { $0 == .home(.map) }) {
                path.removeSubrange(index+1..<path.count)
            }
            if let groupId = appState.groupId {
                path.append(.home(.groupDetail(groupId: groupId)))
            }
        } else {
            path.removeLast()
        }
    }

    
    static func createState(groupId: String?, isOnboarding: Bool) -> SharingStartState {
        SharingStartState(groupId: groupId, isOnboarding: isOnboarding)
    }
}

class SharingStartState: ObservableObject {
    
    let groupId: String?
    let isOnboarding: Bool
    let vm: SharingStartViewModel
    
    init (groupId: String?, isOnboarding: Bool) {
        self.groupId = groupId
        self.isOnboarding = isOnboarding
        self.vm = KoinIOS.ViewModels.shared.sharingStartVM(groupId: groupId, isOnboarding: isOnboarding)
    }
    
    deinit {
        vm.cleanup()
    }
}
