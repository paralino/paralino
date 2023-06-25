//
//  NavigationGraph.swift
//  paralino-ios
//
//  Created by Zelimir on 23.06.23.
//

import Foundation
import shared

enum NavigationGraph: Hashable {
    case settings(SettingsGraph)
    case mapRoot
}

enum WelcomeGraph: Hashable {
    case welcome
    case login
    case signup
}

enum SettingsGraph: Hashable {
    case settings
    case account
    case personalCodes
    case editProfile
    case changeEmail
    case changePassword
    case convertAccount
    case blocklist
    case rawData(type: RawPresentationDataType)
}
