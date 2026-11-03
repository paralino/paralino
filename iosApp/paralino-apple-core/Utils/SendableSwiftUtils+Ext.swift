//
//  SendableSwiftUtils+Ext.swift
//  paralino_apple_core
//
//  Created by Zelimir on 2026-11-03.
//

import Appwrite
import shared

// Logr is set once at startup and it is called from every thread
extension shared.Logr: @retroactive @unchecked Sendable {}

// Kotlin enums are immutable singletons
extension ConditionRequestType: @retroactive @unchecked Sendable {}

// Appwrite SDK is built in Swift 5 mode and does not declare Sendable itself, so it needs this for now
extension Account: @retroactive @unchecked Sendable {}
extension TablesDB: @retroactive @unchecked Sendable {}
extension Functions: @retroactive @unchecked Sendable {}
extension Realtime: @retroactive @unchecked Sendable {}
extension RealtimeSubscription: @retroactive @unchecked Sendable {}
