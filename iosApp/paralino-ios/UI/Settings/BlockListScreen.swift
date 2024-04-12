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
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.BlocklistScreenVC(
                    vm: appState.vm
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.blocklist_screen_title.localizedString())
        }
    }
}

struct BlockListScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.settings(.blocklist)]
    static var previews: some View {
        BlockListScreen(path: $path)
    }
}
