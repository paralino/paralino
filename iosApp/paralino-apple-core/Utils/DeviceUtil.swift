//
//  DeviceUtil.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-30.
//

import Foundation

struct DeviceUtil {
    public static func getMemoryPrintString() -> String {
        let totalMemoryMB = Double(getTotalMemory()) / 1024 / 1024
        let usedMemoryMB = (getUsageMemory() ?? 0) / 1024 / 1024
        
        return String(format: "Memory: %.2f MB in use %.2f MB", totalMemoryMB, usedMemoryMB)
    }
    
    // Returns amount in bytes
    public static func getTotalMemory() -> UInt64 {
        ProcessInfo.processInfo.physicalMemory
    }
    
    // Returns amount in bytes
    public static func getUsageMemory() -> Float? {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size) / 4
        let result = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(
                    mach_task_self_,
                    task_flavor_t(MACH_TASK_BASIC_INFO),
                    $0,
                    &count
                )
            }
        }
        
        guard result == KERN_SUCCESS else { return nil }
        
        return Float(info.resident_size)
    }
}
