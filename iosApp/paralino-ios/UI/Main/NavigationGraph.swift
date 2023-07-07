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
    case home(HomeGraph)
}

enum WelcomeGraph: Hashable {
    case welcome
    case login
    case signup
}

enum HomeGraph: Hashable {
    case map
    case groupInit
    case groupCreate(groupId: String?)
    case groupDetail(groupId: String)
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
