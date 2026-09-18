//
//  PrivacyZonesScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 11.06.26.
//

import SwiftUI
import shared
import MapKit

struct PrivacyZonesScreen: View {

    @Binding var path: [NavigationGraph]
    @StateObject var appState: PrivacyZonesState

    @Environment(\.safeAreaInsets) private var safeAreaInsets

    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.PrivacyZonesScreenVC(
                    vm: appState.vm,
                    paddingValues: AppleScreenPaddingValues(
                        start: safeAreaInsets.start,
                        top: 0,
                        end: safeAreaInsets.end,
                        bottom: safeAreaInsets.bottom
                    ),
                    addNewPrivacyZone: {
                        path.append(.home(.privacyZoneCreate(groupId: appState.groupId, zoneId: nil, editStep: nil, lat: nil, lng: nil)))
                    },
                    openPaywall: {
                        path.append(.settings(.paywall(type: BillingScreenSourceType.upgradeDefault)))
                    },
                    onSkipOnboarding: {
                        path.append(.home(.sharingStart(groupId: appState.groupId, presentation: appState.isOnboarding ? .groupCreateOnboarding : .full)))
                    },
                    openPrivacyZoneDetailScreen: { zone in
                        path.append(.home(.privacyZoneDetail(
                            groupId: GroupOrSpaceIdHelper.shared.unwrap(id: zone.groupOrSpaceId)!,
                            zoneId: zone.id
                        )))
                    },
                    mapUIView: { state in
                        appState.updateMarkers(state: state)
                        let controller = MapComponentVC()
                        controller.mapState = appState
                        return controller
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .highPriorityGesture(
                DragGesture(minimumDistance: 30, coordinateSpace: .local)
                    .onChanged { _ in }
                    .onEnded { _ in }
            )
        }
        .toolbarTitleDisplayMode(.inline)
        .navigationTitle(MR.shared.strings.privacy_zones_list_screen_title.localizedString())
        .navigationBarBackButtonHidden(appState.isOnboarding)
    }

    static func createState(groupId: String, isOnboarding: Bool) -> PrivacyZonesState {
        PrivacyZonesState(groupId: groupId, isOnboarding: isOnboarding)
    }
}

class PrivacyZonesState: BaseMapObservableObject {

    let groupId: String
    let isOnboarding: Bool
    let vm: PrivacyZonesViewModel

    init(groupId: String, isOnboarding: Bool) {
        self.groupId = groupId
        self.isOnboarding = isOnboarding
        self.vm = KoinIOS.ViewModels.shared.privacyZonesVM(groupId: groupId, isOnboarding: isOnboarding)
    }

    func updateMarkers(state: PrivacyZonesListScreenState) {
        markers = state.markers.map { marker in
            MapAnnotatedItem(marker: marker)
        }
        refocusMap(animate: false)
    }

    override func onMarkerTap(markerData: MapPinData) {
        refocusMap(animate: true)
    }

    deinit {
        vm.cleanup()
    }
}
