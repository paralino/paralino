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
                        finishWithSharingScreen()
                    },
                    onSkipOnboarding: {
                        if appState.isGroupJoin {
                            path.removeLast()
                        } else {
                            finishWithSharingScreen()
                        }
                    },
                    openSubscriptions: {
                        path.append(.settings(.paywall(groupId: appState.currentGroupId, type: .upgradeDefault)))
                    },
                    askNotificationPermission: {
                        appDelegate.notificationPermissionHandler.requestNotificationPermission(
                            messageDispather: appState.vm
                        )
                    },
                    askLocationService: {
                        appDelegate.locationPermissionHandler.requestLocationServices(
                            messageDispather: appState.vm
                        )
                    },
                    askLocationPermission: {
                        appDelegate.locationPermissionHandler.requestLocationPermission(
                            messageDispather: appState.vm
                        )
                    },
                    askBackgroundLocationPermission: {
                        appDelegate.locationPermissionHandler.requestLocationPermission(
                            messageDispather: appState.vm
                        )
                    },
                    askBackgroundAccessPermission: {
                        appDelegate.locationPermissionHandler.requestBackgroundRefreshPermission(
                            messageDispather: appState.vm
                        )
                    }
                )
            }
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
        .navigationBarBackButtonHidden(appState.isCreateOnboarding)
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
    
    private func finishWithSharingScreen() {
        if appState.vm.shouldProceedToPaywall {
            if let index = path.firstIndex(where: { $0 == .home(.map) }) {
                path.removeSubrange(index+1..<path.count)
            }
            
            if appState.vm.isPersonalSpace {
                path.append(
                    .settings(
                        .paywall(
                            groupId: appState.currentGroupId,
                            type: appState.isCreateOnboarding ? BillingScreenSourceType.personalSpaceOnboarding : BillingScreenSourceType.upgradeDefault
                        )
                    )
                )
            } else {
                path.append(
                    .settings(
                        .paywall(
                            groupId: appState.currentGroupId,
                            type: appState.isCreateOnboarding ? BillingScreenSourceType.groupOnboarding : BillingScreenSourceType.upgradeDefault
                        )
                    )
                )
            }
        } else if appState.isCreateOnboarding {
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

    
    static func createState(groupId: String?, presentation: SharingStartPresentationType) -> SharingStartState {
        SharingStartState(initialGroupId: groupId, presentation: presentation)
    }
}

@MainActor
class SharingStartState: ObservableObject {
    
    let initialGroupId: String?
    let presentation: SharingStartPresentationType
    // Read by the nonisolated deinit to clean up the view model.
    nonisolated(unsafe) let vm: SharingStartViewModel
    
    var currentGroupId: String? {
        return vm.currentGroupId
    }
    
    var isCreateOnboarding: Bool {
        presentation == SharingStartPresentationType.groupCreateOnboarding
    }
    
    var isGroupJoin: Bool {
        presentation == SharingStartPresentationType.groupJoin
    }
    
    init (initialGroupId: String?, presentation: SharingStartPresentationType) {
        self.initialGroupId = initialGroupId
        self.presentation = presentation
        self.vm = KoinIOS.ViewModels.shared.sharingStartVM(groupId: initialGroupId, presentationType: presentation)
    }
    
    deinit {
        vm.cleanup()
    }
}
