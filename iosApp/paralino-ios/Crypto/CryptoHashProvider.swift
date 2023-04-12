//
//  CryptoHashProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 07.04.23.
//

import Foundation
import shared
import Sodium
import CryptoKit

class CryptoHashProvider: ICryptoHashProvider {
    let sodium = Sodium()
    
    func byteArrayToString(data: KotlinByteArray) -> String {
        return String(decoding: data.toList(), as: UTF8.self)
    }
    
    func computeHash(data: KotlinByteArray, type: HashType) -> shared.Data {
        let content = byteArrayToString(data: data).data(using: .utf8)!
        
        var hash: any Digest
        switch type {
        case .sha256:
            hash = SHA256.hash(data: content)
        case .sha512:
            hash = SHA512.hash(data: content)
        default:
            hash = SHA512.hash(data: content)
        }
        
        let bytes: [UInt8] = Array(hash.makeIterator())
        return bytes.toSharedData()
    }
    
    func fromBase64(data: String) -> shared.Data {
        let content = String(data: Data(base64Encoded: data)!, encoding: .utf8)
        return toByteArray(data: content!)
    }
    
    func toBase64(data: KotlinByteArray) -> String {
        return Data(String(decoding: data.toList(), as: UTF8.self).utf8).base64EncodedString()
    }
    
    func toByteArray(data: String) -> shared.Data {
        return data.bytes.toSharedData()
    }
}

extension KotlinByteArray {
    func toList() -> any Collection<UInt8> {
        var list: [UInt8] = []
        for index in 0..<self.size {
            list.append(self.get(index: index).magnitude)
        }
        return list
    }
    
    func toBytes() -> Bytes {
        return Array(toList())
    }
}

extension Bytes {
    func toSharedData() -> shared.Data {
        return shared.Data(
            content: KotlinByteArray(
                size: Int32(self.count),
                init: { kint in KotlinByte(char: CChar(bitPattern: self[kint.intValue])) }
            )
        )
    }
}
