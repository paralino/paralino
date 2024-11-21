//
//  EditProfileScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 23.06.23.
//

import SwiftUI
import shared

struct EditProfileScreen: View {
    
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.editProfileVM)
    @State var bottomSheet: BottomSheetPickerTypeEmojiPickerBottomSheet? = nil
    @Binding var path: [NavigationGraph]
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.EditProfileScreenVC(
                    vm: appState.vm,
                    onBottomSheetAction: { screen in
                        bottomSheet = screen
                    },
                    onExit: { path.removeLast() }
                )
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .navigationTitle(MR.shared.strings.settings_account_edit_profile.localizedString())
        }
        .photoPickerSheet($bottomSheet) { image in
            appState.vm.onImageChange(image: image)
            bottomSheet = nil
        }
    }
}

struct EditProfileScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.settings(.editProfile)]
    static var previews: some View {
        EditProfileScreen(path: $path)
    }
}
