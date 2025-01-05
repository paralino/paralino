//
//  SubscriptionsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-03.
//

import SwiftUI
import shared

struct GeneralInfoScreen: View {
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.generalInfoVM)
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.GeneralInfoScreenVC(
                    vm: appState.vm,
                    openLicencesScreen: {
                        path.append(.settings(.licences))
                    },
                    onShowOnboarding: {
                        path.append(.settings(.onboarding))
                    },
                    onContactSupport: {
                        let email = BackendResourceIDs.shared.supportEmail
                        if let emailURL = URL(string: "mailto:\(email)?subject=Paralino%20support") {
                            UIApplication.shared.open(emailURL)
                        }
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.settings_section_general_title.localizedString())
        }
    }
}
