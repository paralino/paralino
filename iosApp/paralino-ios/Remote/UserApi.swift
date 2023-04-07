//
//  UserApi.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import Appwrite

class UserApi: IUserApi {
    
    let remoteModule: AppwriteRemoteModule
    
    init(remoteModule: AppwriteRemoteModule) {
        self.remoteModule = remoteModule
    }
    
    
    func changePassword(newPassword: String, oldPassword: String?) async throws -> UserMeRaw {
        return await remoteModule.execute { module in
            try! await module.account.updatePassword(password: newPassword, oldPassword: oldPassword).parseUser()
        }
    }
    
    func confirmEmail(userId: String, secret: String) async throws {
        await remoteModule.execute { module in
            try! await module.account.updateVerification(userId: userId, secret: secret)
        }
    }
    
    
    func deleteAccount() async throws {
        await remoteModule.execute { module in
            try! await module.account.updateStatus()
        }
    }
    
    
    func getAllSessions() async throws -> [UserSession] {
        return await remoteModule.execute { module in
            try! await module.account.listSessions().sessions.map { $0.parseSession() }
        }
    }
    
    func getCurrentSession() async throws -> UserSession {
        return await remoteModule.execute { module in
            try! await module.account.getSession(sessionId: "current").parseSession()
        }
    }
    
    func getMe() async throws -> UserMeRaw {
        return await remoteModule.execute { module in
            try! await module.account.get().parseUser()
        }
    }
    
    func doInitEmailVerification() async throws {
        return await remoteModule.execute { module in
            try! await module.account.createVerification(url: module.backendConfig.resourceIds.emailConfirmationUrl)
        }
    }
    
    func doInitResetPassword(email: String) async throws {
        return await remoteModule.execute { module in
            try! await module.account.createRecovery(email: email, url: module.backendConfig.resourceIds.passwordResetUrl)
        }
    }
    
    func login(email: String, password: String) async throws -> UserSession {
        return await remoteModule.execute { module in
            try! await module.account.createEmailSession(email: email, password: password).parseSession()
        }
    }
    
    func logout() async throws {
        await remoteModule.execute { module in
            try! await module.account.deleteSession(sessionId: "current")
        }
    }
    
    func resetPassword(userId: String, secret: String, password: String) async throws {
        await remoteModule.execute { module in
            try! await module.account.updateRecovery(userId: userId, secret: secret, password: password, passwordAgain: password)
        }
    }
    
    func signUp(userName: String?, email: String, password: String) async throws -> UserMeRaw {
        return await remoteModule.execute { module in
            try! await module.account.create(userId: module.backendConfig.resourceIds.uniqueIdPlaceholder, email: email, password: password, name: userName).parseUser()
        }
    }
    
    func updateEmail(email: String, password: String) async throws -> UserMeRaw {
        return await remoteModule.execute { module in
            try! await module.account.updateEmail(email: email, password: password).parseUser()
        }
    }
    
    func updateUserData(data: [String : Any]) async throws -> UserMeRaw {
        return await remoteModule.execute { module in
            try! await module.account.updatePrefs(prefs: data).parseUser()
        }
    }
}

extension AppwriteModels.Account {
    func parseUser() -> UserMeRaw {
        return UserMeRaw(id: self.id, email: self.email, registration: self.registration, emailVerification: self.emailVerification, data: UserMeDataRaw.companion.parseData(prefs: self.prefs.toMap()))
    }
}

extension AppwriteModels.Session {
    func parseSession() -> UserSession {
        return UserSession(id: self.id, expire: self.expire, deviceName: "iPhone", deviceType: "ios")
    }
}
