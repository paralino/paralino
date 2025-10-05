//
//  UserApi.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import Appwrite

class UserApi: IUserApiIOS {
    
    let remoteModule: AppwriteRemoteModule
    
    init(remoteModule: AppwriteRemoteModule) {
        self.remoteModule = remoteModule
    }
    
    
    func changePassword(newPassword: String, oldPassword: String?) async throws -> Payload<UserMeRaw> {
        return await remoteModule.executeUserApi { module in
            try await module.account.updatePassword(password: newPassword, oldPassword: oldPassword).parseUser()
        }
    }
    
    func confirmEmail(userId: String, secret: String) async throws -> Payload<KotlinUnit> {
        await remoteModule.executeUserApi { module in
            let _ = try await module.account.updateVerification(userId: userId, secret: secret)
            return KotlinUnit()
        }
    }
    
    
    func deleteAccount() async throws -> Payload<KotlinUnit> {
        await remoteModule.executeUserApi { module in
            let _ = try await module.account.updateStatus()
            return KotlinUnit()
        }
    }
    
    func getAllSessions() async throws -> Payload<NSArray> {
        return await remoteModule.executeUserApi { module in
            try await NSArray(array: module.account.listSessions().sessions.map { $0.parseSession() })
        }
    }
    
    func getCurrentSession() async throws -> Payload<UserSession> {
        return await remoteModule.executeUserApi { module in
            try await module.account.getSession(sessionId: "current").parseSession()
        }
    }
    
    func renewCurrentSession() async throws -> Payload<UserSession> {
        return await remoteModule.executeUserApi { module in
            try await module.account.updateSession(sessionId: "current").parseSession()
        }
    }
    
    func getMe() async throws -> Payload<UserMeRaw> {
        return await remoteModule.executeUserApi { module in
            try await module.account.get().parseUser()
        }
    }
    
    func doInitEmailVerification() async throws -> Payload<KotlinUnit> {
        return await remoteModule.executeUserApi { module in
            let _ = try await module.account.createVerification(url: module.appBuildConfig.resourceIds.emailConfirmationUrl)
            return KotlinUnit()
        }
    }
    
    func doInitResetPassword(email: String) async throws -> Payload<KotlinUnit> {
        return await remoteModule.executeUserApi { module in
            let _ = try await module.account.createRecovery(email: email, url: module.appBuildConfig.resourceIds.passwordResetUrl)
            return KotlinUnit()
        }
    }
    
    func login(email: String, password: String) async throws -> Payload<UserSession> {
        return await remoteModule.executeUserApi { module in
            try await module.account.createEmailPasswordSession(email: email, password: password).parseSession()
        }
    }
    
    func resetPassword(userId: String, secret: String, password: String) async throws -> Payload<KotlinUnit> {
        await remoteModule.executeUserApi { module in
            let _ = try await module.account.updateRecovery(userId: userId, secret: secret, password: password)
            return KotlinUnit()
        }
    }
    
    func signUp(userName: String?, email: String, password: String) async throws -> Payload<UserMeRaw> {
        return await remoteModule.executeUserApi { module in
            try await module.account.create(
                userId: module.appBuildConfig.resourceIds.uniqueIdPlaceholder, email: email, password: password, name: userName
            ).parseUser()
        }
    }
    
    func updateEmail(email: String, password: String) async throws -> Payload<UserMeRaw> {
        return await remoteModule.executeUserApi { module in
            try await module.account.updateEmail(email: email, password: password).parseUser()
        }
    }
    
    func updateUserData(data: [String : Any]) async throws -> Payload<UserMeRaw> {
        return await remoteModule.executeUserApi { module in
            try await module.account.updatePrefs(prefs: data).parseUser()
        }
    }
    
    func logoutCurrentSession() async throws -> Payload<KotlinUnit> {
        await remoteModule.executeUserApi { module in
            let _ = try await module.account.deleteSession(sessionId: "current")
            return KotlinUnit()
        }
    }
    
    func logoutSession(sessionId: String) async throws -> Payload<KotlinUnit> {
        await remoteModule.executeUserApi { module in
            let _ = try await module.account.deleteSession(sessionId: sessionId)
            return KotlinUnit()
        }
    }
}

extension AppwriteModels.User {
    func parseUser() -> UserMeRaw {
        let prefsData = try? self.prefs.data.toJson()
        let prefs = prefsData != nil ? UserMeDataRaw.companion.parseData(prefs: SerializationExtKt.fromJsonObjectToMapOfPrimitives(prefsData!)) : nil
        return UserMeRaw(id: self.id, email: self.email, registration: self.registration, emailVerification: self.emailVerification, passwordUpdate: self.passwordUpdate, data: prefs)
    }
}

extension AppwriteModels.Session {
    func parseSession() -> UserSession {
        return UserSession(
            id: self.id,
            createdAt: self.createdAt,
            updatedAt: self.updatedAt,
            expire: self.expire,
            provider: self.provider,
            ip: self.ip,
            osCode: self.osCode,
            osName: self.osName,
            osVersion: self.osVersion,
            clientType: self.clientType,
            clientCode: self.clientCode,
            clientName: self.clientName,
            clientVersion: self.clientVersion,
            clientEngine: self.clientEngine,
            clientEngineVersion: self.clientEngineVersion,
            deviceName: self.deviceName,
            deviceBrand: self.deviceBrand,
            deviceModel: self.deviceModel,
            countryCode: self.countryCode,
            countryName: self.countryName,
            current: self.current
        )
    }
}
