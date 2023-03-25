//
//  Config.swift
//  paralino-ios
//
//  Created by Zelimir on 25.03.23.
//

import Foundation
import shared

class Config: IConfig {
    func clear() {
        
    }
    
    var deviceId: String = ""
    
    var deviceName: String = ""
    
    var dismissedAppVersion: Int64 = Int64(34)
    
    var email: String = ""
    
    var emailConfirmed: Bool = false
    
    var guestEmail: String = ""
    
    var guestPassword: String = ""
    
    var isTokenValid: Bool = false
    
    var pushTokenUnsynced: String = ""
    
    var selectedGroupId: String? = nil
    
    var sessionId: String = ""
    
    var temporaryBackupPassword: String = ""
    
    var userId: String = ""
    
}
