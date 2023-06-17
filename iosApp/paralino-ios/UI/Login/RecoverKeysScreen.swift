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
    @State var alertDialog: AlertDialogData? = nil
    
    var body: some View {
        ComposeRepresentable {
            ScreensIOS.shared.login.RecoverKeysVC(
                vm: appState.vm,
                onShowDialog: { dialog in
                    alertDialog = dialog != nil ? AlertDialogData(coreData: dialog!) : nil
                }
            )
        }.mainAlert($alertDialog) { key in
            if let dialogData = alertDialog?.coreDialog {
                if dialogData is MainAlertDialogRecoveryRecoverAccount {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.recoverAccount()
                    }
                } else if dialogData is MainAlertDialogRecoveryResetAccount {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.resetAccount()
                    }
                } else if dialogData is MainAlertDialogLogout {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.logout()
                    }
                }
            }
        }
    }
}

struct RecoverKeysScreen_Previews: PreviewProvider {
    static var previews: some View {
        RecoverKeysScreen()
    }
}
