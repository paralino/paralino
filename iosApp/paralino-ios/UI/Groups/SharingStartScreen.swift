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
                    onPickGroup: { sheet in groupPicker = sheet },
                    onSharingStarted: {
                        finishWithSharingScreen(isOnboarding: appState.isOnboarding)
                    },
                    onSkipOnboarding: {
                        finishWithSharingScreen(isOnboarding: appState.isOnboarding)
                    },
                    openSubscriptions: {
                        path.append(.settings(.paywall(groupId: appState.currentGroupId, type: .upgrade)))
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
            .highPriorityGesture(
                DragGesture(minimumDistance: 30, coordinateSpace: .local)
                    .onChanged { _ in /* no-op, just to claim */ }
                    .onEnded { _ in }
            )
        }
        .toolbarTitleDisplayMode(.inline)
        .navigationTitle(
            appState.vm.isPersonalSpace
            ? MR.shared.strings.personalSpace_spaceTitle.localizedString()
            : MR.shared.strings.groups_sharing_title.localizedString()
        )
        .navigationBarBackButtonHidden(appState.isOnboarding)
        .groupPickerSheet(
            $groupPicker,
            onSelectGroup: { state in
                groupPicker = nil
                appState.vm.setSelectedGroup(groupOrSpaceId: state.id)
            },
            onEditGroup: { group in },
            onCreateGroup: {},
            onJoinGroup: {},
            onOpenSubscription: {}
        )
    }
    
    private func finishWithSharingScreen(isOnboarding: Bool) {
        if appState.vm.shouldProceedToPaywall {
            if let index = path.firstIndex(where: { $0 == .home(.map) }) {
                path.removeSubrange(index+1..<path.count)
            }
            
            if appState.vm.isPersonalSpace {
                path.append(
                    .settings(
                        .paywall(
                            groupId: appState.currentGroupId,
                            type: isOnboarding ? BillingScreenSourceType.personalSpaceOnboarding : BillingScreenSourceType.upgrade
                        )
                    )
                )
            } else {
                path.append(
                    .settings(
                        .paywall(
                            groupId: appState.currentGroupId,
                            type: isOnboarding ? BillingScreenSourceType.groupOnboarding : BillingScreenSourceType.upgrade
                        )
                    )
                )
            }
        } else if isOnboarding {
            if let index = path.firstIndex(where: { $0 == .home(.map) }) {
                path.removeSubrange(index+1..<path.count)
            }
            
            if !appState.vm.isPersonalSpace {
                if let groupId = appState.currentGroupId {
                    path.append(.home(.groupDetail(groupId: groupId)))
                }
            }
        } else {
            path.removeLast()
        }
    }

    
    static func createState(groupId: String?, isOnboarding: Bool) -> SharingStartState {
        SharingStartState(initialGroupId: groupId, isOnboarding: isOnboarding)
    }
}

class SharingStartState: ObservableObject {
    
    let initialGroupId: String?
    let isOnboarding: Bool
    let vm: SharingStartViewModel
    
    var currentGroupId: String? {
        return vm.currentGroupId
    }
    
    init (initialGroupId: String?, isOnboarding: Bool) {
        self.initialGroupId = initialGroupId
        self.isOnboarding = isOnboarding
        self.vm = KoinIOS.ViewModels.shared.sharingStartVM(groupId: initialGroupId, isOnboarding: isOnboarding)
    }
    
    deinit {
        vm.cleanup()
    }
}
