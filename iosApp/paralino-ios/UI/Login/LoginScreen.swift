//
//  LoginScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 14.04.23.
//

import SwiftUI
import shared

struct LoginScreen: View {
    
    let loginType: LoginScreenUiType
    
    @State var alertDialog: AlertDialogData? = nil
    @StateObject var appState = ViewModelFactoryIOS(
        vm: KoinIOS.ViewModels.shared.loginVM(screenUiType: LoginScreenUiType.signup)
    )
    
    var body: some View {
            VStack {
                ComposeRepresentable {
                    ScreensIOS.shared.login.LoginScreenVC(
                        startScreenUiType: loginType,
                        vm: appState.vm,
                        onShowDialog: { dialog in
                            alertDialog = dialog != nil ? AlertDialogData(coreData: dialog!) : nil
                        },
                        onExit: { }
                    )
                }
                .ignoresSafeArea(.all, edges: .vertical)
                .navigationBarBackButtonHidden(true)
            }
            .mainAlert($alertDialog) { key in
                if let dialogData = alertDialog?.coreDialog {
                    if dialogData is MainAlertDialogGuestAccountCreating {
                        if key == MainAlertDialogButtons.shared.buttonOk {
                            appState.vm.proceed()
                        }
                    } else if dialogData is MainAlertDialogGuestAccountCreating {
                        if key == MainAlertDialogButtons.shared.buttonOk {
                            appState.vm.recoverGuestAccount()
                        } else if key == MainAlertDialogButtons.shared.buttonCancel {
                            appState.vm.dismissGuestAccount()
                        } else  {
                            appState.vm.hideGuestAccountDialog()
                        }
                    }
                }
            }
        }
    
}

struct LoginScreen_Previews: PreviewProvider {
    static var previews: some View {
        LoginScreen(loginType: LoginScreenUiType.signup)
    }
}
