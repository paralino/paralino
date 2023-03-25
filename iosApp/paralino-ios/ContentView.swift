//
//  ContentView.swift
//  paralino-ios
//
//  Created by Zelimir on 23.03.23.
//

import SwiftUI
import shared

struct ContentView: View {
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundColor(.accentColor)
            Text("Hello, world!")
        }
        .padding()
        .task {
            Logr.v(tag: "ContentView", message: "View is shown")
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
