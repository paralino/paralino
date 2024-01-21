//
//  PlacesScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-01-21.
//

import SwiftUI
import shared

struct PlacesScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var appState: ViewModelFactoryIOS<PlacesViewModel>
    let notificationPermissionHandler = NotificationPermissionHandler()
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                
                ScreensIOS.shared.home.PlacesScreenVC(
                    vm: appState.vm,
                    addNewPlace: {},
                    openPlaceDetailScreen: { place in
                        path.append(.home(.placeDetail(groupId: place.groupId, placeId: place.id)))
                    },
                    onNavigateBack: {
                        path.removeLast()
                    },
                    askNotificationPermission: {
                        notificationPermissionHandler.requestNotificationPermission(appDelegate: appDelegate, messageDispather: appState.vm.messageDispatcher)
                    },
                    mapUIView: { state in
                        SwiftUIInUIView(
                            content: VStack {
                                
                            }
                        )
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationBarBackButtonHidden(false)
        }
    }
    
    static func createState(groupId: String) -> ViewModelFactoryIOS<PlacesViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.placesVM(groupId: groupId))
    }
}
