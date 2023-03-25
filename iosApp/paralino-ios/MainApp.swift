//
//  MainApp.swift
//  paralino-ios
//
//  Created by Zelimir on 23.03.23.
//

import SwiftUI

@main
struct MainApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
