//
//  AlertDialog.swift
//  paralino-ios
//
//  Created by Zelimir on 16.04.23.
//

import Foundation
import SwiftUI
import shared

struct AlertDialogData: Identifiable {
    var id: UUID = UUID()
    let title: String
    let message: String
    let buttons: [AlertDialogButton]
    let coreDialog: MainAlertDialog
    
    init(coreData: shared.MainAlertDialog) {
        title = coreData.dialogData.title
        message = coreData.dialogData.message
        buttons = coreData.dialogData.buttons.map { buttonData in
            AlertDialogButton(
                key: buttonData.id,
                role: buttonData.id == MainAlertDialogButtons.shared.buttonOk ? nil : .none,
                text: buttonData.text
            )
        }
        coreDialog = coreData
    }
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
    

    static func buttonOK() -> AlertDialogButton {
        AlertDialogButton(key: MainAlertDialogButtons.shared.buttonOk, text: "button_ok")
    }
    static func buttonCancel() -> AlertDialogButton {
        AlertDialogButton(key: MainAlertDialogButtons.shared.buttonCancel, role: .cancel, text: "button_cancel")
    }
    static func buttonsOKCancel() -> [AlertDialogButton] { [buttonOK(), buttonCancel()] }
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
