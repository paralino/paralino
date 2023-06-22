//
//  SettingsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 22.06.23.
//

import SwiftUI
import shared

struct SettingsScreen: View {
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.settingsVM)
    @State var shareText: ShareText?
    @State private var openAccount = false
    @State private var openPersonalCodes = false
    
    var body: some View {
        ZStack {
            NavigationLink(
                destination: AccountScreen(),
                isActive: $openAccount
            ) { EmptyView() }
            NavigationLink(
                destination: PersonalCodeScreen(),
                isActive: $openPersonalCodes
            ) { EmptyView() }
            ComposeRepresentable {
                ScreensIOS.shared.settings.SettingsScreenVC(
                    vm: appState.vm,
                    onShareApp: { data in
                        shareText = ShareText(text: data)
                    },
                    openConvertAccount: {
                        
                    },
                    openProfileDetails: {
                        openAccount = true
                    },
                    openPersonalCodes: {
                        openPersonalCodes = true
                    },
                    openBlockList: {
                        
                    },
                    openLicences: {
                        
                    }
                )
            }
        }
        .sheet(item: $shareText) { shareText in
            ActivityView(text: shareText.text)
        }
        .ignoresSafeArea(.all, edges: .bottom)
    }
}

struct SettingsScreen_Previews: PreviewProvider {
    static var previews: some View {
        SettingsScreen()
    }
}
