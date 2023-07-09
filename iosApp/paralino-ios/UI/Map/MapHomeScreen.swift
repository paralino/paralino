//
//  MapHomeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared

struct MapHomeScreen: View {
    
    @Binding var path: [NavigationGraph]
    
    var body: some View {
        VStack {
            Button("Open Profile") {
                path.append(.settings(.settings))
            }
            .navigationBarBackButtonHidden(true)
            ImageModelImage(imageModel: ImageModel.companion.emoji(emoji: Emojis.shared.robot), useBackgroundForEmoji: true)
                .frame(width: 150, height: 150)
                
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }
}

struct MapHomeScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.home(.map)]
    static var previews: some View {
        MapHomeScreen(path: $path)
    }
}
