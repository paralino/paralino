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
import paralino_apple_core

class BackgroundRefresh {
    
    static var refreshEverything: RefreshEverythingUseCase = { KoinIOS.Instances.shared.refreshEverything }()
    static var pushProvider: IPushProviderManager = { KoinIOS.Instances.shared.pushProvider }()
    
    static func scheduleAppRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: "app.paralino.ios.task.refresh.full")
        request.earliestBeginDate = Date(timeIntervalSinceNow: 12 * 60 * 60)
        
        do {
            try BGTaskScheduler.shared.submit(request)
            Logr.i(tag:"delegate", message: "App refresh scheduled")
        } catch {
            Logr.w(tag:"delegate", message: "Could not schedule app refresh: \(error)")
        }
    }
    
    static func registerAppRefresh() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: "app.paralino.ios.task.refresh.full", using: nil) { task in
            Logr.i(tag:"delegate", message: "App refresh task start")
            self.scheduleAppRefresh()
            
            // notifications will be synced in full refresh, so no need to sync it sooner
            Logr.i(tag:"delegate", message: "Reschedule notification sync")
            self.cancelNotificationsRefresh()
            self.scheduleNotificationsRefresh()
            
            DispatchQueue.main.async {
                Task {
                    Logr.i(tag:"delegate", message: "App refresh async task start")
                    try? await self.refreshEverything.refreshEverything(forceGroupRefresh: false, minimumDelayMillis: 4 * 60 * 60 * 1000)
                    Logr.i(tag:"delegate", message: "App refresh task done")
                    
                    task.setTaskCompleted(success: true)
                }
            }
        }
    }
    
    static func scheduleNotificationsRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: "app.paralino.ios.task.refresh.notifications")
        request.earliestBeginDate = Date(timeIntervalSinceNow: 2 * 60 * 60)
        
        do {
            try BGTaskScheduler.shared.submit(request)
            Logr.i(tag:"delegate", message: "Notifications refresh scheduled")
        } catch {
            Logr.w(tag:"delegate", message: "Could not schedule notifications refresh: \(error)")
        }
    }
    
    static func registerNotificationsRefresh() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: "app.paralino.ios.task.refresh.notifications", using: nil) { task in
            Logr.i(tag:"delegate", message: "Notifications refresh task start")
            self.scheduleNotificationsRefresh()
            pushProvider.syncPushNotifications()
            Logr.i(tag:"delegate", message: "Notifications refresh task done")
            task.setTaskCompleted(success: true)
        }
    }
    
    static func cancelFullRefresh() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: "app.paralino.ios.task.refresh.full")
    }
    
    static func cancelNotificationsRefresh() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: "app.paralino.ios.task.refresh.notifications")
    }
    
    static func logout() {
        BGTaskScheduler.shared.cancelAllTaskRequests()
    }
}
