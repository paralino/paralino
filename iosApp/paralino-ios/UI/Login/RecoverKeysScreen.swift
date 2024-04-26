//
//  RecoverKeysScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared

struct RecoverKeysScreen: View {
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.recoverKeysVM)
    
    var body: some View {
        ComposeRepresentable {
            ScreensIOS.shared.login.RecoverKeysVC(
                vm: appState.vm
            )
        }
    }
}

struct RecoverKeysScreen_Previews: PreviewProvider {
    static var previews: some View {
        RecoverKeysScreen()
    }
}
