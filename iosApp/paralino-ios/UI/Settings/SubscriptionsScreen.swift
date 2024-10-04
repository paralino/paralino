//
//  SubscriptionsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-03.
//

import SwiftUI
import shared

struct SubscriptionsScreen: View {
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.subscriptionVM)
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.SubscriptionsScreenVC(
                    vm: appState.vm,
                    onNavigateBack: {
                        path.removeLast()
                    },
                    openBillingStore: {
                        
                    },
                    openPaywall: {
                        
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.plan_limit_alert_title.localizedString())
        }
    }
}
