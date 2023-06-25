//
//  PersonalCodeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 22.06.23.
//

import SwiftUI
import shared

struct PersonalCodeScreen: View {
    
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.personalCodeVM)
    @State var alertDialog: AlertDialogData? = nil
    @State var shareText: ShareText?
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.PersonalCodeScreenVC(
                    vm: appState.vm,
                    onShowDialog: { dialog in
                        alertDialog = dialog != nil ? AlertDialogData(coreData: dialog!) : nil
                    },
                    onShare: { data in
                        shareText = ShareText(text: data)
                    },
                    onNavigateBack: {}
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
        }
        .mainAlert($alertDialog) { key in
            if let dialogData = alertDialog?.coreDialog {
                if dialogData is MainAlertDialogPersonalCodeDelete {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.deletePersonalCode()
                    }
                }
            }
        }
        .sheet(item: $shareText) { shareText in
            ActivityView(text: shareText.text)
        }
    }
}

struct PersonalCodeScreen_Previews: PreviewProvider {
    static var previews: some View {
        PersonalCodeScreen()
    }
}
