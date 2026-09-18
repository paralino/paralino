//
//  GroupJoinWelcomeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 18.09.26.
//

import SwiftUI
import shared

struct GroupJoinWelcomeScreen: View {

    @Binding var path: [NavigationGraph]
    @StateObject var appState: ViewModelFactoryIOS<GroupJoinWelcomeViewModel>

    @Environment(\.safeAreaInsets) private var safeAreaInsets

    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.GroupJoinWelcomeScreenVC(
                    vm: appState.vm,
                    paddingValues: safeAreaInsets,
                    openSharing: {
                        path.append(
                            .home(
                                .sharingStart(
                                    groupId: appState.vm.groupId,
                                    presentation: .groupJoin
                                )
                            )
                        )
                    },
                    openMap: {
                        path.removeAll()
                        path.append(.home(.map))
                    }
                )
            }
            .navigationBarBackButtonHidden(true)
            .ignoresSafeArea(.all, edges: .vertical)
            .highPriorityGesture(
                DragGesture(minimumDistance: 30, coordinateSpace: .local)
                    .onChanged { _ in }
                    .onEnded { _ in }
            )
        }
    }

    static func createState(groupId: String) -> ViewModelFactoryIOS<GroupJoinWelcomeViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.groupJoinWelcomeVM(groupId: groupId))
    }
}
