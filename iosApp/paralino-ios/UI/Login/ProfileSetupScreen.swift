//
//  ProfileSetupScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 14.06.23.
//

import SwiftUI
import shared

struct ProfileSetupScreen: View {
    
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.editProfileVM)
    @State var bottomSheet: BottomSheetPickerTypeEmojiPickerBottomSheet? = nil
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.login.CompleteProfileVC(
                    vm: appState.vm
                ) { screen in
                    bottomSheet = screen
                }
            }
            .toolbarTitleDisplayMode(.inline)
        }
        .photoPickerSheet($bottomSheet) { image in
            appState.vm.onImageChange(image: image)
            bottomSheet = nil
        }
    }
}

struct ProfileSetupScreen_Previews: PreviewProvider {
    static var previews: some View {
        ProfileSetupScreen()
    }
}
