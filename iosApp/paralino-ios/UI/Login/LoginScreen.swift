//
//  LoginScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 14.04.23.
//

import SwiftUI
import shared

struct LoginScreen: View {
    
    @State var alertDialog: AlertDialogData? = nil
    @StateObject var appState: ViewModelFactoryIOS<LoginViewModel>
    
    var body: some View {
            ZStack {
                Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
                ComposeRepresentable {
                    ScreensIOS.shared.login.LoginScreenVC(
                        vm: appState.vm,
                        onShowDialog: { dialog in
                            alertDialog = dialog != nil ? AlertDialogData(coreData: dialog!) : nil
                        },
                        onExit: { }
                    )
                }
                .ignoresSafeArea(.all, edges: .bottom)
//                .navigationBarBackButtonHidden(true)
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
    @StateObject static var loginAppState = ViewModelFactoryIOS(
        vm: KoinIOS.ViewModels.shared.loginVM(screenUiType: LoginScreenUiType.signup)
    )
    static var previews: some View {
        LoginScreen(appState: loginAppState)
    }
}
