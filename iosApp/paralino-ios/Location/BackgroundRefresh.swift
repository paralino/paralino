//
//  BackgroundRefresh.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-04-04
//

import Foundation
import BackgroundTasks
import UserNotifications
import shared

class BackgroundRefresh {
    
    static var refreshEverything: RefreshEverythingUseCase = { KoinIOS.Instances.shared.refreshEverything }()
    
    static func scheduleAppRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: "app.paralino.ios.task.refresh")
        request.earliestBeginDate = Date(timeIntervalSinceNow: 60 * 60)
        
        do {
            try BGTaskScheduler.shared.submit(request)
            Logr.i(tag:"delegate", message: "App Refresh scheduled")
        } catch {
            Logr.w(tag:"delegate", message: "Could not schedule app refresh: \(error)")
        }
    }
    
    static func registerAppRefresh() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: "app.paralino.ios.task.refresh", using: nil) { task in
            Logr.i(tag:"delegate", message: "App refresh task start")
            self.scheduleAppRefresh()
            DispatchQueue.main.async {
                let runTaks = Task {
                    Logr.i(tag:"delegate", message: "App refresh async task start")
                    try? await self.refreshEverything.refreshEverything(forceGroupRefresh: false, minimumDelayMillis: 4 * 60 * 60)
                    Logr.i(tag:"delegate", message: "App refresh task done")
                    
                    let content = UNMutableNotificationContent()
                    content.title = "Paralino refresh"
                    content.body = "Refreshed"
                    content.interruptionLevel = .passive
                    
                    let request = UNNotificationRequest(identifier: "app_refresh", content: content, trigger: nil)
                    try? await UNUserNotificationCenter.current().add(request)
                    
                    task.setTaskCompleted(success: true)
                }
            }
        }
    }
}
