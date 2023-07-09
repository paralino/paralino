//
//  MapHomeState.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import Foundation
import shared
import SwiftUI

class MapHomeState : ObservableObject {
    
    let vm: MapHomeViewModel
    
    @Published var groupState: Bool? = nil
    @Published var path: [NavigationGraph] = []
    @Published var uiState: MapHomeScreenState = MapHomeScreenState.Empty(currentUserImage: ImageModel.companion.placeholder())
    
    init() {
        vm = KoinIOS.ViewModels.shared.mapHomeVM
        vm.observe(flow: vm.allGroupsState) { [weak self] state in
            if let state = state as? Bool {
                self?.groupState = state
                if state {
                    if self?.path.isEmpty != false {
                        self?.path = [.home(.map)]
                    } else {
                        self?.path[0] = .home(.map)
                    }
                } else {
                    self?.path = [.home(.groupInit)]
                }
            } else {
                self?.groupState = nil
            }
        }
        
        vm.observe(flow: vm.uiState) { [weak self] state in
            if let state = state as? MapHomeScreenState {
                self?.uiState = state
            }
        }
    }
    
    deinit {
        vm.cleanup()
    }
}
