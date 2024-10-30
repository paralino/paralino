//
//  FreePlanInfoScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-03.
//

import SwiftUI
import shared

struct FreePlanInfoScreen: View {
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.planInfoVM)
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.FreePlanInfoScreenVC(
                    vm: appState.vm,
                    showBanner: true,
                    onContinue: {
                        path[path.count - 1] = .home(.groupCreate(groupId: nil))
                    },
                    openPaywall: {
                        path.append(.settings(.paywall(type: BillingScreenSourceType.freePlanNotice)))
                    }
                )
            }
            .navigationBarBackButtonHidden(true)
        }
    }
}
