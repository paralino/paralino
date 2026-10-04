//
//  AppThemeManager.swift
//  paralino-ios
//
//  Created by Zelimir on 2026-10-04.
//

import SwiftUI
import shared

enum AppThemeManager {
    
    /// Overrides the interface style of all app windows, so SwiftUI, UIKit and
    /// Compose (`isSystemInDarkTheme()`) content follows the in-app theme setting.
    static func apply(theme: AppThemeType = AppPrefs.Companion.shared.instance.appTheme) {
        let style = theme.userInterfaceStyle
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .forEach { $0.overrideUserInterfaceStyle = style }
    }
}

extension AppThemeType {
    
    var userInterfaceStyle: UIUserInterfaceStyle {
        if self == .light {
            return .light
        } else if self == .dark {
            return .dark
        } else {
            return .unspecified
        }
    }
}
