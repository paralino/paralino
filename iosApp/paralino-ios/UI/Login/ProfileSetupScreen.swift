//
//  ProfileSetupScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 14.06.23.
//

import SwiftUI
import shared

struct ProfileSetupScreen: View {
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.editProfileVM)
    @State var bottomSheet: BottomSheetsScreen? = nil
    @State var bottomSheetResult: Any? = nil
    var isShowingSheet: Binding<Bool> {
        Binding(get: { bottomSheet != nil }, set: { _ in bottomSheet = nil })
    }
    
    let icons: [String] = ["sun.max.fill", "moon.fill", "star.fill", "heart.fill", "bolt.fill", "cloud.fill", "person.fill", "book.fill", "gamecontroller.fill", "camera.fill"]
    @State private var selectedIconIndex: Int?
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.login.CompleteProfileVC(
                    vm: appState.vm
                ) { screen in
                    bottomSheet = screen
                }
            }
        }
        .ignoresSafeArea(.all, edges: .bottom)
        .sheet(isPresented: isShowingSheet) {
            VStack {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8, alignment: .center), count: 6), spacing: 8) {
                    ForEach(icons.indices, id: \.self) { index in
                        Button(action: {
                            selectedIconIndex = index
                            bottomSheet = nil
                        }) {
                            Image(systemName: icons[index])
                                .imageScale(.large)
                                .padding(2)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .aspectRatio(1.0, contentMode: .fill)
                        .background(selectedIconIndex == index ? Color.blue.opacity(0.5) : .themeSurface)
                        .cornerRadius(10)
                    }
                }
                .padding()
                .presentationDetents([.medium, .large])
                Spacer()
            }
        }
    }
}

struct ProfileSetupScreen_Previews: PreviewProvider {
    static var previews: some View {
        ProfileSetupScreen()
    }
}
