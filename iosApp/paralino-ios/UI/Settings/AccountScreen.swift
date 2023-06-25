//
//  AccountScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 22.06.23.
//

import SwiftUI
import shared

struct AccountScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.settingsVM)
    @State var alertDialog: AlertDialogData? = nil
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.AccountScreenVC(
                    vm: appState.vm,
                    onShowDialog: { dialog in
                        alertDialog = dialog != nil ? AlertDialogData(coreData: dialog!) : nil
                    },
                    openProfileRawScreen: {
                        path.append(.settings(.rawData(type: .UserPresentation())))
                    },
                    openConvertAccount: {
                        path.append(.settings(.convertAccount))
                    },
                    openEditProfile: {
                        path.append(.settings(.editProfile))
                    },
                    openChangeEmail: {
                        path.append(.settings(.changeEmail))
                    },
                    openChangePassword: {
                        path.append(.settings(.changePassword))
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
        }
        .mainAlert($alertDialog) { key in
            if let dialogData = alertDialog?.coreDialog {
                if dialogData is MainAlertDialogVerifyEmail {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.sendEmailVerification()
                    }
                } else if dialogData is MainAlertDialogDeleteAccount {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                            alertDialog = AlertDialogData(coreData: MainAlertDialogDeleteAccountConfirm())
                        }
                    }
                } else if dialogData is MainAlertDialogDeleteAccountConfirm {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.deleteAccount()
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

struct AccountScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.settings(.account)]
    static var previews: some View {
        AccountScreen(path: $path)
    }
}
