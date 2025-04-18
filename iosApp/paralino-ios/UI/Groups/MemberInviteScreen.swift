//
//  MemberInviteScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 07.07.23.
//

import SwiftUI
import shared

struct MemberInviteScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var appState: MemberInviteState
    
    @State var groupPicker: BottomSheetPickerTypeGroupPickerBottomSheet? = nil
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.MemberInviteScreenVC(
                    vm: appState.vm,
                    isOnboarding: appState.isOnboarding,
                    onPickGroup: { sheet in groupPicker = sheet },
                    openPaywall: { type in
                        path.append(.settings(.paywall(type: type)))
                    },
                    onMemberAdded: {
                        if appState.isOnboarding {
                            path.append(.home(HomeGraph.places(groupId: appState.groupId!, isOnboarding: appState.isOnboarding)))
                        } else {
                            path.removeLast()
                        }
                    },
                    onSkipOnboarding: {
                        path.append(.home(HomeGraph.places(groupId: appState.groupId!, isOnboarding: appState.isOnboarding)))
                    }
                )
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
        }
        .toolbarTitleDisplayMode(.inline)
        .navigationTitle(MR.shared.strings.invite_connect_screen_title.localizedString())
        .navigationBarBackButtonHidden(appState.isOnboarding)
        .groupPickerSheet(
            $groupPicker,
            onSelectGroup: { state in
                groupPicker = nil
                appState.vm.selectGroup(group: state)
            },
            onEditGroup: { group in },
            onCreateGroup: {},
            onJoinGroup: {},
            onOpenSubscription: {}
        )
    }
    
    static func createState(groupId: String?, code: String?, isOnboarding: Bool) -> MemberInviteState {
        MemberInviteState(groupId: groupId, code: code, isOnboarding: isOnboarding)
    }
}

class MemberInviteState: ObservableObject {
    
    let groupId: String?
    let code: String?
    let isOnboarding: Bool
    let vm: MemberInviteViewModel
    
    init (groupId: String?, code: String?, isOnboarding: Bool) {
        self.groupId = groupId
        self.code = code
        self.isOnboarding = isOnboarding
        self.vm = KoinIOS.ViewModels.shared.memberInviteVM(groupId: groupId, code: code)
    }
    
    deinit {
        vm.cleanup()
    }
}
