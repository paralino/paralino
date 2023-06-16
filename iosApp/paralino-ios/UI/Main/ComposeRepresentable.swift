//
//  ComposeRepresentable.swift
//  paralino-ios
//
//  Created by Zelimir on 16.06.23.
//

import Foundation
import SwiftUI
import shared

struct ComposeRepresentable: UIViewControllerRepresentable {
    let provider: () -> UIViewController
    func makeUIViewController(context: Context) -> UIViewController {
        provider()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

class ViewModelFactoryIOS<T: CoreViewModel> : ObservableObject {
    let vm: T
    
    init(vm: T) {
        self.vm = vm
    }
    
    deinit {
        vm.cleanup()
    }
}
