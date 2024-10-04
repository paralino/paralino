//
//  SettingsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 22.06.23.
//

import SwiftUI
import shared

struct SettingsScreen: View {
    
    @Binding var path: [NavigationGraph]
    
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.settingsVM)
    @State var shareText: ShareText?
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.SettingsScreenVC(
                    vm: appState.vm,
                    onShareApp: { data in
                        shareText = ShareText(text: data)
                    },
                    openConvertAccount: {
                        path.append(.settings(.convertAccount))
                    },
                    openProfileDetails: {
                        path.append(.settings(.account))
                    },
                    openPersonalCodes: {
                        path.append(.settings(.personalCodes))
                    },
                    openBlockList: {
                        path.append(.settings(.blocklist))
                    },
                    openProfileRawScreen: {
                        path.append(.settings(.rawData(type: .UserPresentation())))
                    },
                    openLicences: {
                        
                    },
                    openAppStatus: {
                        path.append(.settings(.appStatusDebug))
                    },
                    openSubscriptions: {
                        path.append(.settings(.subscriptions))
                    },
                    openPaywall: {
                        
                    },
                    onShare: { data in
                        shareText = ShareText(text: data)
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.title_settings.localizedString())
        }
        .sheet(item: $shareText) { shareText in
            ActivityView(text: shareText.text)
        }
    }
}

struct SettingsScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.settings(.settings)]
    static var previews: some View {
        SettingsScreen(path: $path)
    }
}
