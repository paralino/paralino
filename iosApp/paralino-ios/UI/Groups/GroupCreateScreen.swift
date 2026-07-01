//
//  GroupCreateScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 07.07.23.
//

import SwiftUI
import shared

struct GroupCreateScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var appState: ViewModelFactoryIOS<CreateGroupViewModel>
    @State var bottomSheet: BottomSheetPickerTypeEmojiPickerBottomSheet? = nil
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.GroupCreateScreenVC(
                    vm: appState.vm,
                    onBottomSheetAction: { screen in
                        bottomSheet = screen
                    },
                    groupUpdated: { newGroupId in
                        if appState.vm.isGroupCreate() {
                            path[path.count - 1] = .home(HomeGraph.memberInvite(groupId: newGroupId, code: nil, isOnboarding: true))
                        } else {
                            path.removeLast()
                        }
                    }
                )
            }
        }
        .photoPickerSheet($bottomSheet) { image in
            appState.vm.updateImage(image: image)
            bottomSheet = nil
        }
    }
    
    static func createState(groupId: String?) -> ViewModelFactoryIOS<CreateGroupViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.groupCreateVM(groupId: groupId))
    }
}

struct GroupCreateScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.home(.groupCreate(groupId: nil))]
    static var previews: some View {
        GroupCreateScreen(path: $path, appState: GroupCreateScreen.createState(groupId: nil))
    }
}
