//
//  AlertDialog.swift
//  paralino-ios
//
//  Created by Zelimir on 16.04.23.
//

import Foundation
import SwiftUI


struct AlertDialogData: Identifiable {
    var id: UUID = UUID()
    let title: String
    let message: String
    let buttons: [AlertDialogButton]
}

struct AlertDialogButton: Identifiable {
    var id: String { key }
    let key: String
    let role: ButtonRole?
    let text: String
    
    init(key: String, text: String) {
        self.key = key
        self.role = nil
        self.text = text
    }
    
    init(key: String, role: ButtonRole?, text: String) {
        self.key = key
        self.role = role
        self.text = text
    }
    
    static let keyPositive: String = "button.positive"
    static let keyNegative: String = "button.negative"
    static func buttonOK() -> AlertDialogButton { AlertDialogButton(key: keyPositive, text: "button_ok") }
    static func buttonCancel() -> AlertDialogButton { AlertDialogButton(key: keyNegative, role: .cancel, text: "button_cancel") }
    static func buttonsOKCancel() -> [AlertDialogButton] { [buttonOK(), buttonCancel()] }
}

extension AlertDialogData {
    static func guestAccountWarning() -> AlertDialogData {
        AlertDialogData(title: "guest_account_title", message: "guest_account_message", buttons: [AlertDialogButton.buttonOK(), AlertDialogButton.buttonCancel()])
    }
    static func genericError() -> AlertDialogData {
        AlertDialogData(title: "alert_error_generic_title", message: "alert_error_generic_message", buttons: [AlertDialogButton.buttonOK()])
    }
}

struct AlertDialog: ViewModifier {
    
    @Binding var alertData: AlertDialogData?
    var onAction: ((String) -> Void)?
    
    var isShowingAlert: Binding<Bool> {
        Binding(get: { alertData != nil }, set: { _ in alertData = nil })
    }
    
    func body(content: Content) -> some View {
        content
            .alert(
                alertData?.title ?? "",
                isPresented: isShowingAlert,
                presenting: alertData
            ) { details in
                ForEach(details.buttons) { button in
                    Button(button.text, role: button.role) {
                        onAction?(button.key)
                    }
                }
            } message: { details in
                Text(details.message)
            }
    }
}

extension View {
    func mainAlert(_ alertData: Binding<AlertDialogData?>, onAction: @escaping (String) -> Void) -> some View {
        self.modifier(AlertDialog(alertData: alertData, onAction: onAction))
    }
}
