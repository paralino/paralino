//
//  MapHomeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared
import paralino_apple_core

struct MapRootScreen: View {
    
    @EnvironmentObject var mainAppState: MainAppObject
    @StateObject var initState = GroupInitState()
    
    var body: some View {
        NavigationStack(path: $mainAppState.userReadyGraph) {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
                .navigationDestination(for: NavigationGraph.self) { route in
                    ZStack {
                        switch route {
                        case let .settings(settingsGraph):
                            switch settingsGraph {
                            case .account: AccountScreen(path: $mainAppState.userReadyGraph)
                            case .personalCodes: PersonalCodeScreen()
                            case .settings: SettingsScreen(path: $mainAppState.userReadyGraph)
                            case .changeEmail: ChangeEmailScreen(onFinish: {})
                            case .changePassword: ChangePasswordScreen(path: $mainAppState.userReadyGraph)
                            case .convertAccount: ConvertAccountScreen(path: $mainAppState.userReadyGraph)
                            case .blocklist: BlockListScreen(path: $mainAppState.userReadyGraph)
                            case .userDevices: UserDevicesScreen(path: $mainAppState.userReadyGraph)
                            case .rawData(let type):
                                RawDataScreen(appState: RawDataScreen.createState(type: type))
                            case .editProfile: EditProfileScreen(path: $mainAppState.userReadyGraph)
                            case .appStatusDebug: AppStatusDebugScreen(path: $mainAppState.userReadyGraph)
                            case .appStatus: AppStatusScreen(path: $mainAppState.userReadyGraph)
                            case .licences: LicencesScreen(path: $mainAppState.userReadyGraph)
                            case .subscriptions: SubscriptionsScreen(path: $mainAppState.userReadyGraph)
                            case .paywall(let groupId, let type): PaywallScreen(path: $mainAppState.userReadyGraph, state: PaywallScreenState(groupId: groupId, type: type))
                            case .generalInfo: GeneralInfoScreen(path: $mainAppState.userReadyGraph)
                            case .locationSettings: LocationSettingsScreen(path: $mainAppState.userReadyGraph)
                            case .onboarding: WelcomeOnboardingScreen(
                                isDemoMode: true,
                                onFinish: {
                                    mainAppState.userReadyGraph.removeLast()
                                }
                            ) 
                            case .appSettings: AppSettingsScreen(path: $mainAppState.userReadyGraph)
                            case .appThemeSettings: GenericSettingsScreen(path: $mainAppState.userReadyGraph, appState: GenericSettingsScreenState(vm: KoinIOS.ViewModels.shared.appThemeVM))
                            case .measurementSystemSettings: GenericSettingsScreen(path: $mainAppState.userReadyGraph, appState: GenericSettingsScreenState(vm: KoinIOS.ViewModels.shared.measurementSystemVM))
                            case .notificationGroupingSettings: GenericSettingsScreen(path: $mainAppState.userReadyGraph, appState: GenericSettingsScreenState(vm: KoinIOS.ViewModels.shared.notificationGroupingVM))
                            case .mapInteractionsSettings: GenericSettingsScreen(path: $mainAppState.userReadyGraph, appState: GenericSettingsScreenState(vm: KoinIOS.ViewModels.shared.mapInteractionsVM))
                            case .navigationAppSettings: GenericSettingsScreen(path: $mainAppState.userReadyGraph, appState: GenericSettingsScreenState(vm: KoinIOS.ViewModels.shared.navigationAppVM))
                            case .placeSearchSettings: GenericSettingsScreen(path: $mainAppState.userReadyGraph, appState: GenericSettingsScreenState(vm: KoinIOS.ViewModels.shared.placeSearchSettingsVM))
                            case .personalSpaceSettings: GenericSettingsScreen(path: $mainAppState.userReadyGraph, appState: GenericSettingsScreenState(vm: KoinIOS.ViewModels.shared.personalSpaceFeatureSettingsVM))
                            case .licenceCode: LicenceCodeScreen(path: $mainAppState.userReadyGraph)
                            }
                        case let .home(homeGraph):
                            switch homeGraph {
                            case .groupCreate(let groupId): GroupCreateScreen(path: $mainAppState.userReadyGraph, appState: GroupCreateScreen.createState(groupId: groupId))
                            case .groupDetail(let groupId): GroupDetailScreen(path: $mainAppState.userReadyGraph, appState: GroupDetailScreen.createState(groupId: groupId))
                            case .groupSettings(let groupId): GroupSettingsScreen(path: $mainAppState.userReadyGraph, appState: GroupSettingsScreen.createState(groupId: groupId))
                            case .batteryNotificationsSettings(let groupId): BatteryNotificationsSettingsScreen(path: $mainAppState.userReadyGraph, appState: BatteryNotificationsSettingsScreen.createState(groupId: groupId))
                            case .sharingStart(let groupId, let presentation):
                                SharingStartScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: SharingStartScreen.createState(groupId: groupId, presentation: presentation)
                                )
                            case .map: MapHomeScreen(path: $mainAppState.userReadyGraph)
                            case .groupInit: AccountOnboardingScreen(path: $mainAppState.userReadyGraph, appState: initState)
                            case .groupJoinWelcome(let groupId):
                                GroupJoinWelcomeScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: GroupJoinWelcomeScreen.createState(groupId: groupId)
                                )
                            case .memberInvite(let groupId, let code, let isOnboarding):
                                MemberInviteScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: MemberInviteScreen.createState(groupId: groupId, code: code, isOnboarding: isOnboarding)
                                )
                            case .places(let groupId, let isOnboarding):
                                PlacesScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: PlacesScreen.createState(groupId: groupId, isOnboarding: isOnboarding)
                                )
                            case .placeDetail(groupId: let groupId, placeId: let placeId):
                                PlaceDetailScreen(path: $mainAppState.userReadyGraph, appState: PlaceDetailScreen.createState(groupId: groupId, placeId: placeId))
                            case .placeAlertsSettings(groupId: let groupId, placeId: let placeId):
                                PlaceAlertsSettingsScreen(path: $mainAppState.userReadyGraph, appState: PlaceDetailScreen.createState(groupId: groupId, placeId: placeId))
                            case .placeSensitivitySettings(groupId: let groupId, placeId: let placeId):
                                GenericSettingsScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: GenericSettingsScreenState(
                                        vm: KoinIOS.ViewModels.shared.placeSensitivityVM(groupId: groupId, placeId: placeId)
                                    )
                                )
                            case .placeCreate(groupId: let groupId, placeId: let placeId, editStep: let editStep, lat: let lat, lng: let lng):
                                PlaceCreateScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: PlaceCreateScreen.createState(
                                        groupId: groupId,
                                        placeId: placeId,
                                        editStep: editStep,
                                        lat: lat,
                                        lng: lng
                                    )
                                )
                            case .privacyZones(let groupId, let isOnboarding):
                                PrivacyZonesScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: PrivacyZonesScreen.createState(groupId: groupId, isOnboarding: isOnboarding)
                                )
                            case .privacyZoneDetail(groupId: let groupId, zoneId: let zoneId):
                                PrivacyZoneDetailScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: PrivacyZoneDetailScreen.createState(groupId: groupId, zoneId: zoneId)
                                )
                            case .privacyZoneCreate(groupId: let groupId, zoneId: let zoneId, editStep: let editStep, lat: let lat, lng: let lng):
                                PrivacyZoneCreateScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: PrivacyZoneCreateScreen.createState(
                                        groupId: groupId,
                                        zoneId: zoneId,
                                        editStep: editStep,
                                        lat: lat,
                                        lng: lng
                                    )
                                )
                            case .freePlanInfo: FreePlanInfoScreen(path: $mainAppState.userReadyGraph)
                            case .locationHistorySettings(groupId: let groupId): LocationHistoryDeleteScreen(path: $mainAppState.userReadyGraph, appState: LocationHistoryDeleteScreen.createState(groupId: groupId))
                            case .personalSpace: PersonalSpaceScreen(path: $mainAppState.userReadyGraph, appState: PersonalSpaceScreen.createState())
                            case .personalSpaceSettings: PersonalSpaceSettingsScreen(path: $mainAppState.userReadyGraph)
                            }
                        }
                    }.onAppear {
                        Logr.d(message: "Route: \(route)")
                    }
                }
        }
        .onChange(of: initState.groupState, initial: true) { oldValue, newValue in
            if newValue == .map {
                if mainAppState.userReadyGraph.isEmpty == true {
                    mainAppState.userReadyGraph = [.home(.map)]
                } else {
                    mainAppState.userReadyGraph[0] = .home(.map)
                }
            } else if newValue == .groupInit {
                if mainAppState.userReadyGraph.isEmpty == true {
                    mainAppState.userReadyGraph = [.home(.groupInit)]
                } else {
                    mainAppState.userReadyGraph[0] = .home(.groupInit)
                }
            }
        }
    }
}

