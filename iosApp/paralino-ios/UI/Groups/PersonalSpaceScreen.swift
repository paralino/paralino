//
//  GroupDetailScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 07.07.23.
//

import SwiftUI
import shared

struct PersonalSpaceScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var appState: ViewModelFactoryIOS<PersonalSpaceViewModel>
    @State var alertDialog: AlertDialogData? = nil
    
    @Environment(\.safeAreaInsets) private var safeAreaInsets
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.PersonalSpaceScreenVC(
                    vm: appState.vm,
                    paddingValues: AppleScreenPaddingValues(
                        start: safeAreaInsets.start,
                        top: 0,
                        end: safeAreaInsets.end,
                        bottom: safeAreaInsets.bottom
                    ),
                    openSettings: {
                        path.append(.home(.personalSpaceSettings))
                    },
                    openTimeline: {
                        path.append(.home(.locationHistory(groupId: UserGroup.companion.groupPersonalDevicesId, memberUserId: "")))
                    },
                    openPlaces: {
                        path.append(.home(.places(groupId: UserGroup.companion.groupPersonalDevicesId, isOnboarding: false)))
                    },
                    openSharing: {
                        path.append(.home(.sharingStart(groupId: UserGroup.companion.groupPersonalDevicesId, isOnboarding: false)))
                    },
                    openPaywall: {
                        path.append(.settings(.paywall(type: BillingScreenSourceType.upgrade)))
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
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.personalSpace_spaceTitle.localizedString())
        }
    }
    
    static func createState() -> ViewModelFactoryIOS<PersonalSpaceViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.personalSpaceVM)
    }
}
