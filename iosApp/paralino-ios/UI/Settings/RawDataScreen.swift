//
//  RawDataScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 23.06.23.
//

import SwiftUI
import shared

struct RawDataScreen: View {
    
    @StateObject var appState: ViewModelFactoryIOS<RawDataPresentationViewModel>
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.RawDataScreenVC(
                    vm: appState.vm
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.raw_data_screen_title.localizedString())
        }
    }
    
    static func createState(type: RawPresentationDataType) -> ViewModelFactoryIOS<RawDataPresentationViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.rawDataVM(type: type))
    }
}

struct RawDataScreen_Previews: PreviewProvider {
    static var previews: some View {
        RawDataScreen(appState: RawDataScreen.createState(type: .UserPresentation()))
    }
}
