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
    @State var bottomSheet: BottomSheetsScreen.EmojiPicker? = nil
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.EditProfileScreenVC(
                    vm: appState.vm,
                    onBottomSheetAction: { screen in
                        bottomSheet = screen as? BottomSheetsScreen.EmojiPicker
                    },
                    onExit: { }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
        }
        .photoPickerSheet($bottomSheet) { image in
            appState.vm.onImageChange(image: image)
            bottomSheet = nil
        }
    }
}

struct EditProfileScreen_Previews: PreviewProvider {
    static var previews: some View {
        EditProfileScreen()
    }
}
