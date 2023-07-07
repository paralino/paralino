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
    @StateObject var appState: ViewModelFactoryIOS<MemberInviteViewModel>
    
    // TODO add group picker
    @State var bottomSheet: BottomSheetsScreen.GroupPicker? = nil
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.MemberInviteScreenVC(
                    vm: appState.vm,
                    onPickGroup: { sheet in bottomSheet = sheet },
                    onExit: { path.removeLast() }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
        }
    }
    
    static func createState(groupId: String?, code: String?) -> ViewModelFactoryIOS<MemberInviteViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.memberInviteVM(groupId: groupId, code: code))
    }
}

struct MemberInviteScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.home(.groupCreate(groupId: nil))]
    static var previews: some View {
        MemberInviteScreen(path: $path, appState: MemberInviteScreen.createState(groupId: nil, code: nil))
    }
}
