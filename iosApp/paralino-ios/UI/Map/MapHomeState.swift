//
//  MapHomeState.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import Foundation
import shared

class MapHomeState : ObservableObject {
    
    let vm: MapHomeViewModel
    @Published
    var groupState: Bool? = nil
    
    init() {
        vm = KoinIOS.ViewModels.shared.mapHomeVM
        vm.observe(flow: vm.allGroupsState) { [weak self] state in
            if let state = state as? Bool {
                self?.groupState = state
            } else {
                self?.groupState = nil
            }
        }
    }
    
    deinit {
        vm.cleanup()
    }
}
