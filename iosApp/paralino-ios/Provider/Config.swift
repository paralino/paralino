//
//  Config.swift
//  paralino-ios
//
//  Created by Zelimir on 25.03.23.
//

import Foundation
import shared

class Config: IConfig {
    
    override func clear() {
        UserDefaults.standard.removeObject(forKey: IConfig.companion.keyToken)
        UserDefaults.standard.removeObject(forKey: IConfig.companion.keyDeviceName)
        UserDefaults.standard.removeObject(forKey: IConfig.companion.keyUserId)
        UserDefaults.standard.removeObject(forKey: IConfig.companion.keyEmail)
        UserDefaults.standard.removeObject(forKey: IConfig.companion.keyEmailConfirmed)
        UserDefaults.standard.removeObject(forKey: IConfig.companion.keyTempPassword)
        UserDefaults.standard.removeObject(forKey: IConfig.companion.keyUserKey)
        UserDefaults.standard.removeObject(forKey: IConfig.companion.keyPushTokenUnsynced)
        UserDefaults.standard.removeObject(forKey: IConfig.companion.keySelectedGroupID)
    }
    
    override var deviceId: String {
        get {
            return UserDefaults.standard.string(forKey: IConfig.companion.keyDeviceID) ?? ""
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyDeviceID)
        }
    }
    
    override var deviceName: String {
        get {
            return UserDefaults.standard.string(forKey: IConfig.companion.keyDeviceName) ?? ""
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyDeviceName)
        }
    }
    
    override var dismissedAppVersion: Int64 {
        get {
            return Int64(UserDefaults.standard.integer(forKey: IConfig.companion.keyDismissedAppVersionID))
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyDismissedAppVersionID)
        }
    }
    
    // TODO move to keychain after testing
    override var userKey: String {
        get {
            return UserDefaults.standard.string(forKey: IConfig.companion.keyUserKey) ?? ""
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyUserKey)
        }
    }
    
    // TODO move to keychain after testing
    override var email: String {
        get {
            return UserDefaults.standard.string(forKey: IConfig.companion.keyEmail) ?? ""
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyEmail)
        }
    }
    
    override var emailConfirmed: Bool {
        get {
            return UserDefaults.standard.bool(forKey: IConfig.companion.keyEmailConfirmed)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyEmailConfirmed)
        }
    }
    
    // TODO move to keychain after testing
    override var guestEmail: String {
        get {
            return UserDefaults.standard.string(forKey: IConfig.companion.keyEmailGuest) ?? ""
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyEmailGuest)
        }
    }
    
    // TODO move to keychain after testing
    override var guestPassword: String {
        get {
            return UserDefaults.standard.string(forKey: IConfig.companion.keyPasswordGuest) ?? ""
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyPasswordGuest)
        }
    }
    
    // TODO move to keychain after testing
    override var temporaryBackupPassword: String {
        get {
            return UserDefaults.standard.string(forKey: IConfig.companion.keyTempPassword) ?? ""
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyTempPassword)
        }
    }
    
    
    // TODO move to keychain after testing
    override var pushTokenUnsynced: String {
        get {
            return UserDefaults.standard.string(forKey: IConfig.companion.keyPushTokenUnsynced) ?? ""
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyPushTokenUnsynced)
        }
    }
    
    override var selectedGroupId: String? {
        get {
            return UserDefaults.standard.string(forKey: IConfig.companion.keySelectedGroupID)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keySelectedGroupID)
        }
    }
    
    override var sessionId: String {
        get {
            return UserDefaults.standard.string(forKey: IConfig.companion.keyToken) ?? ""
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyToken)
        }
    }
    
    override var userId: String {
        get {
            return UserDefaults.standard.string(forKey: IConfig.companion.keyUserId) ?? ""
        }
        set {
            UserDefaults.standard.set(newValue, forKey: IConfig.companion.keyUserId)
        }
    }
    
}
