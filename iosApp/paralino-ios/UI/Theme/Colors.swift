//
//  Colors.swift
//  paralino-ios
//
//  Created by Zelimir on 15.04.23.
//

import Foundation
import SwiftUI

struct AppColors {
    struct Raw {
        static let accent = 0xFF26A69A
        static let accentLight = 0xFF80CBC4
        static let accentDark = 0xFF00796B
        
        static let lightBackground = 0xFFFAFAFA
        static let lightSurface = 0xFFF1F3EF
        
        static let lightThemePrimary = 0xFF12423D
        static let lightThemeSecondary = 0xFFCCCCCC
        
        static let errorBackground = 0x33F44336
        static let redMain = 0xFFB3261E
        
        static let darkBackground = 0xFF000E0A
        static let darkSurface = 0xFF0B1A17
        
        static let darkThemePrimary = 0xFF0F443F
        static let darkThemeSecondary = 0xFF929292
    }
}

extension Color {
    
    init(withAlphaRGB argb: Int) {
        self.init(
            red: Double((argb >> 16) & 0xFF) / 255,
            green: Double((argb >> 8) & 0xFF) / 255,
            blue: Double(argb & 0xFF) / 255,
            opacity: Double((argb >> 24) & 0xFF) / 255
        )
    }
    
    static func getColor(lightArgb: Int, darkArgb: Int) -> Color {
        Color(UIColor.create(lightArgb: lightArgb, darkArgb: darkArgb))
    }
    
    static let accent = Color(withAlphaRGB: AppColors.Raw.accent)
    static let accentLight = Color(withAlphaRGB: AppColors.Raw.accentLight)
    static let accentDark = Color(withAlphaRGB: AppColors.Raw.accentDark)
    
    static let lightBackground = Color(withAlphaRGB: AppColors.Raw.lightBackground)
    static let lightSurface = Color(withAlphaRGB: AppColors.Raw.lightSurface)
    
    static let lightThemePrimary = Color(withAlphaRGB: AppColors.Raw.lightThemePrimary)
    static let lightThemeSecondary = Color(withAlphaRGB: AppColors.Raw.lightThemeSecondary)
    
    static let errorBackground = Color(withAlphaRGB: AppColors.Raw.errorBackground)
    static let redMain = Color(withAlphaRGB: AppColors.Raw.redMain)
    
    static let darkBackground = Color(withAlphaRGB: AppColors.Raw.darkBackground)
    static let darkSurface = Color(withAlphaRGB: AppColors.Raw.darkSurface)
    
    static let darkThemePrimary = Color(withAlphaRGB: AppColors.Raw.darkThemePrimary)
    static let darkThemeSecondary = Color(withAlphaRGB: AppColors.Raw.darkThemeSecondary)
    
    static var themePrimary: Color {
        getColor(lightArgb: AppColors.Raw.lightThemePrimary, darkArgb: AppColors.Raw.darkThemePrimary)
    }
    static var themeSecondary: Color {
        getColor(lightArgb: AppColors.Raw.lightThemeSecondary, darkArgb: AppColors.Raw.darkThemeSecondary)
    }
    static var themebackground: Color {
        getColor(lightArgb: AppColors.Raw.lightBackground, darkArgb: AppColors.Raw.darkBackground)
    }
    static var themeSurface: Color {
        getColor(lightArgb: AppColors.Raw.lightSurface, darkArgb: AppColors.Raw.darkSurface)
    }
    static var themeErrorBackground: Color {
        getColor(lightArgb: AppColors.Raw.errorBackground, darkArgb: AppColors.Raw.errorBackground)
    }
    
    static var themeOnPrimary: Color {
        getColor(lightArgb: AppColors.Raw.lightBackground, darkArgb: AppColors.Raw.lightBackground)
    }
    static var themeOnSecondary: Color {
        getColor(lightArgb: AppColors.Raw.darkBackground, darkArgb: AppColors.Raw.lightBackground)
    }
    static var themeOnBackground: Color {
        getColor(lightArgb: AppColors.Raw.darkBackground, darkArgb: AppColors.Raw.lightBackground)
    }
    static var themeOnSurface: Color {
        getColor(lightArgb: AppColors.Raw.darkBackground, darkArgb: AppColors.Raw.lightBackground)
    }
    static var themeOnError: Color {
        getColor(lightArgb: AppColors.Raw.redMain, darkArgb: AppColors.Raw.redMain)
    }
}

extension UIColor {
    
    static func create(lightArgb: Int, darkArgb: Int) -> UIColor {
        UIColor { $0.userInterfaceStyle == .dark ? UIColor(withAlphaRGB: darkArgb) : UIColor(withAlphaRGB: lightArgb) }
    }
    
    convenience init(rgb: Int, alpha: CGFloat = 1.0) {
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: alpha
        )
    }
    
    convenience init(withAlphaRGB argb: Int) {
        self.init(
            red: CGFloat((argb >> 16) & 0xFF) / 255,
            green: CGFloat((argb >> 8) & 0xFF) / 255,
            blue: CGFloat(argb & 0xFF) / 255,
            alpha: CGFloat((argb >> 24) & 0xFF) / 255
        )
    }
}
