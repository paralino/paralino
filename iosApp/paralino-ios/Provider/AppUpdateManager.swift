//
//  File.swift
//  paralino-ios
//
//  Created by Zelimir Bosnjak on 01.04.24.
//

import Foundation
import shared
import UIKit

class AppUpdateManager: IAppUpdateManager {
    override func startUpdateProcess(data: ConfigData) {
        if let url = URL(string: BackendResourceIDs.shared.storeUrlDeeplinkAppStore) {
            UIApplication.shared.open(url)
        }
    }
}
