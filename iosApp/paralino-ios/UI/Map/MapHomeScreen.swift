//
//  MapHomeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI

struct MapHomeScreen: View {
    
    @Binding var path: [NavigationGraph]
    
    var body: some View {
        Button("Open Profile") {
            path.append(.settings(.settings))
        }
        .navigationBarBackButtonHidden(true)
    }
}

struct MapHomeScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.home(.map)]
    static var previews: some View {
        MapHomeScreen(path: $path)
    }
}
