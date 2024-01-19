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

class SwiftUIInUIView<Content: View>: UIView {

    init(content: Content) {
        super.init(frame: CGRect())
        let hostingController = UIHostingController(rootView: content)
        hostingController.view.translatesAutoresizingMaskIntoConstraints = false
        addSubview(hostingController.view)
        NSLayoutConstraint.activate([
            hostingController.view.topAnchor.constraint(equalTo: topAnchor),
            hostingController.view.leadingAnchor.constraint(equalTo: leadingAnchor),
            hostingController.view.trailingAnchor.constraint(equalTo: trailingAnchor),
            hostingController.view.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
