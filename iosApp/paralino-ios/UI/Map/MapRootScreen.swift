//
//  MapHomeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared

struct MapRootScreen: View {
    @StateObject var mapState = MapHomeState()
    
    var body: some View {
        switch mapState.groupState {
        case nil: ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
        case true: MapHomeScreen()
        case false: GroupInitScreen()
        case .some(_):
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
        }
    }
}

struct MapRootScreen_Previews: PreviewProvider {
    static var previews: some View {
        MapRootScreen()
    }
}
