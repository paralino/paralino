//
//  PlaceDetailScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-01-21.
//

import SwiftUI
import shared

struct PlaceDetailScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var appState: ViewModelFactoryIOS<PlaceDetailViewModel>
    let notificationPermissionHandler = NotificationPermissionHandler()
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                
                ScreensIOS.shared.home.PlaceDetailScreenVC(
                    vm: appState.vm,
                    openPlaceRawDataScreen: {
                        path.append(.settings(.rawData(type: RawPresentationDataType.GroupPlacePresentation(placeId: appState.vm.placeId))))
                    },
                    openPlaceEditScreen: {
                        
                    },
                    onNavigateBack: {
                        path.removeLast()
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
    
    static func createState(groupId: String, placeId: String) -> ViewModelFactoryIOS<PlaceDetailViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.placeDetailVM(groupId: groupId, placeId: placeId))
    }
}

