//
//  BlockListScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 23.06.23.
//

import SwiftUI
import shared

struct BlockListScreen: View {
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.blocklistVM)
    @State var alertDialog: AlertDialogData? = nil
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.BlocklistScreenVC(
                    vm: appState.vm,
                    onShowDialog: { dialog in
                        alertDialog = dialog != nil ? AlertDialogData(coreData: dialog!) : nil
                    },
                    onNavigateBack: { path.removeLast() }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
        }
        .mainAlert($alertDialog) { key in
            if let dialogData = alertDialog?.coreDialog {
                if let dialog = dialogData as? MainAlertDialogUnblockGroupOrUser {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.removeBlock(id: dialog.id)
                    }
                }
            }
        }
    }
}

struct BlockListScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.settings(.blocklist)]
    static var previews: some View {
        BlockListScreen(path: $path)
    }
}
