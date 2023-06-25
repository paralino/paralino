//
//  GroupInitScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared

struct GroupInitScreen: View {
    
    @Binding var path: [NavigationGraph]

    @EnvironmentObject var appDelegate: AppDelegate
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.groupInitVM)
    @State var alertDialog: AlertDialogData? = nil
    @State var shareText: ShareText?
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.GroupInitScreenVC(
                    vm: appState.vm,
                    onShowDialog: { dialog in
                        alertDialog = dialog != nil ? AlertDialogData(coreData: dialog!) : nil
                    },
                    openSettings: { path.append(.settings(.settings)) },
                    onCreateGroup: {},
                    openPersonalCodes: { path.append(.settings(.personalCodes)) },
                    onSharePersonalCode: { data in
                        shareText = ShareText(text: data)
                    },
                    askNotificationPermission: {
                        let center = UNUserNotificationCenter.current()
                        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
                            appDelegate.locationStatusProvider.hasNotificationPermission = granted
                            appDelegate.locationStatusProvider.refreshLocationStatus()
                        }
                    },
                    onClose: {}
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
                }
            }
        }
        .sheet(item: $shareText) { shareText in
            ActivityView(text: shareText.text)
        }
    }
}

struct GroupInitScreen_Previews: PreviewProvider {
    @State static var graph: [NavigationGraph] = []
    static var previews: some View {
        GroupInitScreen(path: $graph)
    }
}
