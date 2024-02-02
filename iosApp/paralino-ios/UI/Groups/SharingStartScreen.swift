//
//  SharingStartScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2023-11-24.
//

import SwiftUI
import shared

struct SharingStartScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @EnvironmentObject var locationManager: LocationManager
    @Binding var path: [NavigationGraph]
    @StateObject var appState: ViewModelFactoryIOS<SharingStartViewModel>
    
    @State var bottomSheet: BottomSheetsScreen.GroupPicker? = nil
    let notificationPermissionHandler = NotificationPermissionHandler()
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.StartSharingScreenVC(
                    vm: appState.vm,
                    groupId: nil,
                    onPickGroup: { sheet in bottomSheet = sheet },
                    onNavigateBack: {
                        path.removeLast()
                    },
                    askNotificationPermission: {
                        notificationPermissionHandler.requestNotificationPermission(appDelegate: appDelegate, messageDispather: appState.vm.messageDispatcher)
                    }, 
                    askLocationService: {
                        
                    },
                    askLocationPermission: {
                        locationManager.requestLocationPermission()
                    },
                    askBackgroundLocationPermission: {
                        locationManager.requestAlwaysOnLocationPermission()
                    }
                )
            }
        }
    }
    
    static func createState(groupId: String?) -> ViewModelFactoryIOS<SharingStartViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.sharingStartVM(groupId: groupId))
    }
}

