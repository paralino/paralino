//
//  GroupInitScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared

struct GroupInitScreen: View {
    
    @Binding var path: [NavigationGraph]

    @EnvironmentObject var appDelegate: AppDelegate
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.groupInitVM)
    @State var shareText: ShareText?
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.GroupInitScreenVC(
                    vm: appState.vm,
                    openSettings: { path.append(.settings(.settings)) },
                    onCreateGroup: { path.append(.home(.groupCreate(groupId: nil))) },
                    openFreePlanInfo: { path.append(.home(.freePlanInfo)) },
                    openPersonalCodes: { path.append(.settings(.personalCodes)) },
                    onSharePersonalCode: { data in
                        shareText = ShareText(text: data)
                    },
                    askNotificationPermission: {
                        appDelegate.notificationPermissionHandler.requestNotificationPermission(
                            messageDispather: appState.vm.messageDispatcher
                        )
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
        }
        .sheet(item: $shareText) { shareText in
            ActivityView(text: shareText.text)
        }
        .navigationBarBackButtonHidden(true)
    }
}

struct GroupInitScreen_Previews: PreviewProvider {
    @State static var graph: [NavigationGraph] = []
    static var previews: some View {
        GroupInitScreen(path: $graph)
    }
}
