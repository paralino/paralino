//
//  GroupInitScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared

struct AccountOnboardingScreen: View {
    
    @Binding var path: [NavigationGraph]

    @EnvironmentObject var appDelegate: AppDelegate
    @StateObject var appState: GroupInitState
    @State var shareText: ShareText?
    @Environment(\.safeAreaInsets) private var safeAreaInsets
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.AccountOnboardingScreenVC(
                    vm: appState.vm,
                    paddingValues:  AppleScreenPaddingValues(
                        start: safeAreaInsets.start,
                        top: 0,
                        end: safeAreaInsets.end,
                        bottom: safeAreaInsets.bottom
                    ),
                    onCreateGroup: { path.append(.home(.groupCreate(groupId: nil))) },
                    openPersonalCodes: { path.append(.settings(.personalCodes)) },
                    openPaywall: { type in
                        path.append(.settings(.paywall(groupId: nil, type: type)))
                    },
                    onSharePersonalCode: { data in
                        shareText = ShareText(text: data)
                    },
                    openSharingScreen: { groupOrSpace in
                        path.append(.home(.sharingStart(groupId: GroupOrSpaceIdHelper.shared.unwrap(id: groupOrSpace), presentation: .groupCreateOnboarding)))
                    },
                    askNotificationPermission: {
                        appDelegate.notificationPermissionHandler.requestNotificationPermission(
                            messageDispather: appState.vm
                        )
                    },
                    openGroupJoinWelcome: { groupId in
                        path.append(.home(.groupJoinWelcome(groupId: groupId)))
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
        }
        .sheet(item: $shareText) { shareText in
            ActivityView(text: shareText.text)
        }
        .navigationBarBackButtonHidden(true)
    }
}

@MainActor
class GroupInitState: ObservableObject {
    
    @Published var groupState: State = .loading
    // Read by the nonisolated deinit to clean up the view model.
    nonisolated(unsafe) let vm: AccountOnboardingViewModel
    
    init() {
        self.vm = KoinIOS.ViewModels.shared.accountOnboardingVM
        
        vm.observe(flow: vm.canDisplayMap) { [weak self] value in
            let state = ResourceSwift<KotlinBoolean>(data: value)
            switch state {
            case .notInitialized: self?.groupState = .loading
            case .ready(let data): if data.boolValue { self?.groupState = .map } else { self?.groupState = .groupInit }
            }
        }
    }
    
    deinit {
        vm.cleanup()
    }
    
    public enum State {
        case groupInit
        case map
        case loading
    }
}
