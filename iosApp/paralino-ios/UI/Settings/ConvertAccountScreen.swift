//
//  ConvertAccountScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 23.06.23.
//

import SwiftUI
import shared

struct ConvertAccountScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.convertAccountVM)
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.ConvertAccountScreenVC(
                    vm: appState.vm,
                    onNavigateBack: { path.removeLast() }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.settings_account_guest_convert.localizedString())
        }
    }
}

struct ConvertAccountScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.settings(.convertAccount)]
    static var previews: some View {
        ConvertAccountScreen(path: $path)
    }
}
