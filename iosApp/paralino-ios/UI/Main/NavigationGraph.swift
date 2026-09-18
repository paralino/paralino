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

enum EmailVerificationGraph: Hashable {
    case verifyEmail
    case changeEmail
}

enum WelcomeGraph: Hashable {
    case welcome
    case onboarding
    case login
    case signup
    case passwordReset(deeplink: DeeplinkType.PasswordReset)
}

enum HomeGraph: Hashable {
    case map
    case groupInit
    case groupJoinWelcome(groupId: String)
    case freePlanInfo
    case groupCreate(groupId: String?)
    case groupDetail(groupId: String)
    case groupSettings(groupId: String)
    case batteryNotificationsSettings(groupId: String)
    case sharingStart(groupId: String?, presentation: SharingStartPresentationType)
    case memberInvite(groupId: String?, code: String?, isOnboarding: Bool)
    case places(groupId: String, isOnboarding: Bool)
    case placeDetail(groupId: String, placeId: String)
    case placeAlertsSettings(groupId: String, placeId: String)
    case placeSensitivitySettings(groupId: String, placeId: String)
    case placeCreate(groupId: String, placeId: String?, editStep: CreatePlaceScreenStep?, lat: Double? = nil, lng: Double? = nil)
    case privacyZones(groupId: String, isOnboarding: Bool)
    case privacyZoneDetail(groupId: String, zoneId: String)
    case privacyZoneCreate(groupId: String, zoneId: String?, editStep: CreatePrivacyZoneScreenStep?, lat: Double? = nil, lng: Double? = nil)
    case personalSpace
    case personalSpaceSettings
    case locationHistorySettings(groupId: String)
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
    case onboarding
    case appSettings
    case measurementSystemSettings
    case notificationGroupingSettings
    case mapInteractionsSettings
    case navigationAppSettings
    case placeSearchSettings
    case licenceCode
}
