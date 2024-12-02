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
    case passwordReset(deeplink: DeeplinkType.PasswordReset)
}

enum HomeGraph: Hashable {
    case map
    case groupInit
    case freePlanInfo
    case groupCreate(groupId: String?)
    case groupDetail(groupId: String)
    case groupSettings(groupId: String)
    case sharingStart(groupId: String?, isOnboarding: Bool)
    case memberInvite(groupId: String?, code: String?, isOnboarding: Bool)
    case places(groupId: String, isOnboarding: Bool)
    case placeDetail(groupId: String, placeId: String)
    case placeCreate(groupId: String, placeId: String?, lat: Double? = nil, lng: Double? = nil)
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
    case subscriptions
    case paywall(groupId: String? = nil, type: BillingScreenSourceType)
    case rawData(type: RawPresentationDataType)
    case appStatusDebug
    case licences
    case generalInfo
    case locationSettings
}
