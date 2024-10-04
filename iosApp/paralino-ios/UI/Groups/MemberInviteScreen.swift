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
    
    // TODO add group picker
    @State var bottomSheet: BottomSheetPickerTypeGroupPickerBottomSheet? = nil
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.MemberInviteScreenVC(
                    vm: appState.vm,
                    onPickGroup: { sheet in bottomSheet = sheet },
                    onMemberAdded: {
                        if appState.isOnboarding {
                            path[path.count - 1] = .home(HomeGraph.places(groupId: appState.groupId!, isOnboarding: appState.isOnboarding))
                        } else {
                            path.removeLast()
                        }
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.invite_connect_screen_title.localizedString())
        }
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
