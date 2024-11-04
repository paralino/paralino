//
//  PaywallScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-03.
//

import SwiftUI
import shared

struct PaywallScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var state: PaywallScreenState
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.PaywallScreenVC(
                    vm: state.vm,
                    onNavigateBack: { isSuccess in
                        goBack(isSuccess: isSuccess.boolValue)
                    },
                    onStartPurchase: { event in
                        state.startPurchase(event)
                    },
                    onRestorePurchase: { event in
                        state.restorePurchase(event)
                    }
                )
            }
            .navigationTitle(MR.shared.strings.plan_limit_alert_title.localizedString())
            .toolbar(.hidden, for: .navigationBar)
        }
    }
    
    private func goBack(isSuccess: Bool) {
        switch state.type {
        case .onboarding:
            if let groupId = state.groupId, !groupId.isEmpty {
                if let index = path.firstIndex(where: { $0 == .home(.map) }) {
                    path.removeSubrange(index+1..<path.count)
                }
                path.append(.home(.groupDetail(groupId: groupId)))
            } else {
                path.append(.home(.map))
            }

        case .freePlanNotice:
            if isSuccess {
                if let index = path.firstIndex(where: { $0 == .home(.map) }) {
                    path.removeSubrange(index+1..<path.count)
                }
                path.append(.home(.groupCreate(groupId: nil)))
            } else {
                path.removeLast()
            }

        case .normal:
            path.removeLast()
            
        default: break
        }
    }

}

class PaywallScreenState: ObservableObject {
    
    let groupId: String?
    let type: BillingScreenSourceType
    let vm: PaywallViewModel
    
    init (groupId: String?, type: BillingScreenSourceType) {
        self.groupId = groupId
        self.type = type
        self.vm = KoinIOS.ViewModels.shared.paywallVM(type: type)
    }
    
    func startPurchase(_ event: PaywallScreenEvent.OnStartPurchase) {
        DispatchQueue.main.async { [weak self] in
            Task {
                do {
                    guard let billingProvider = self?.vm.billingProvider as? BillingProviderIOS else { return }
                    try await billingProvider.startPurchase(planType: event.planType, newProductPlanId: event.newPlanProductId)
                    self?.vm.onEvent(event: PaywallScreenUiEvent.RefreshPlans())
                } catch {
                    if let error = error as? PurchaseError {
                        self?.vm.onEvent(event: PaywallScreenUiEvent.OnPurchaseError(error: error.sharedError))
                    } else {
                        let sharedError = PurchaseErrorException(errorCode: 0, errorMessage: "Unknown error", userCancelled: false)
                        self?.vm.onEvent(event: PaywallScreenUiEvent.OnPurchaseError(error: sharedError))
                    }
                }
            }
        }
    }
    
    func restorePurchase(_ event: PaywallScreenEvent.OnRestorePurchase) {
        DispatchQueue.main.async { [weak self] in
            Task {
                do {
                    guard let billingProvider = self?.vm.billingProvider as? BillingProviderIOS else { return }
                    try await billingProvider.restorePurchase()
                    self?.vm.onEvent(event: PaywallScreenUiEvent.RefreshPlans())
                } catch {
                    if let error = error as? PurchaseError {
                        self?.vm.onEvent(event: PaywallScreenUiEvent.OnPurchaseError(error: error.sharedError))
                    } else {
                        let sharedError = PurchaseErrorException(errorCode: 0, errorMessage: "Unknown error", userCancelled: false)
                        self?.vm.onEvent(event: PaywallScreenUiEvent.OnPurchaseError(error: sharedError))
                    }
                }
            }
        }
    }
    
    deinit {
        vm.cleanup()
    }
}
