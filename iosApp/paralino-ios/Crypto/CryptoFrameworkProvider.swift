//
//  CryptoFrameworkProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 07.04.23.
//

import Foundation
import shared
import Sodium
import CryptoKit

class CryptoFrameworkProvider: ICryptoServiceProvider, ICryptoHashProvider {
    
    let sodium: Sodium
    
    init(sodium: Sodium) {
        self.sodium = sodium
    }
    
    init() {
        sodium = Sodium()
    }
    
    
    func createNewSymmetricKey() -> String {
        return sodium.secretBox.key().toHex(sodium)
    }
    
    func createNewCryptoKeyPair() -> KeyPair {
        let keys = sodium.box.keyPair()
        return KeyPair(
            privateKey: keys!.secretKey.toHex(sodium),
            publicKey: keys!.publicKey.toHex(sodium)
        )
    }
    
    func createNewSignatureKeyPair() -> KeyPair {
        let keys = sodium.sign.keyPair()
        return KeyPair(
            privateKey: keys!.secretKey.toHex(sodium),
            publicKey: keys!.publicKey.toHex(sodium)
        )
    }
    
    func decryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String) -> String {
        sodium.box.open(
            nonceAndAuthenticatedCipherText: data.hex2Bin(sodium),
            senderPublicKey: publicKey.hex2Bin(sodium),
            recipientSecretKey: myPrivateKey.hex2Bin(sodium)
        )!.utf8String!
    }
    
    func decryptDataWithPassword(data: String, password: String, context: String) -> String {
        let salt = sodium.genericHash.hash(
            message: context.bytes,
            outputLength: sodium.genericHash.BytesMin
        )
        
        let key = sodium.pwHash.hash(
            outputLength: sodium.secretBox.KeyBytes,
            passwd: password.bytes,
            salt: salt!,
            opsLimit: sodium.pwHash.OpsLimitInteractive,
            memLimit: sodium.pwHash.MemLimitInteractive
        )
        
        return decryptDataWithSymmetricKey(data: data, key: key!.toHex(sodium))
    }
    
    func decryptDataWithSymmetricKey(data: String, key: String) -> String {
        sodium.secretBox.open(
            nonceAndAuthenticatedCipherText: data.hex2Bin(sodium),
            secretKey: key.hex2Bin(sodium)
        )!.utf8String!
    }
    
    func encryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String) -> String {
        sodium.box.seal(
            message: data.bytes,
            recipientPublicKey: publicKey.hex2Bin(sodium),
            senderSecretKey: myPrivateKey.hex2Bin(sodium)
        )!.toHex(sodium)
    }
    
    func encryptDataWithPassword(data: String, password: String, context: String) -> String {
        
        let salt = sodium.genericHash.hash(
            message: context.bytes,
            outputLength: sodium.genericHash.BytesMin
        )
        
        let key = sodium.pwHash.hash(
            outputLength: sodium.secretBox.KeyBytes,
            passwd: password.bytes,
            salt: salt!,
            opsLimit: sodium.pwHash.OpsLimitInteractive,
            memLimit: sodium.pwHash.MemLimitInteractive
        )
        
        return encryptDataWithSymmetricKey(data: data, key: key!.toHex(sodium))
    }
    
    func encryptDataWithSymmetricKey(data: String, key: String) -> String {
        sodium.secretBox.seal(
            message: data.bytes,
            secretKey: key.hex2Bin(sodium)
        )!.toHex(sodium)
    }
    
    func signData(data: String, myPrivateKey: String) -> String {
        sodium.sign.sign(message: data.bytes, secretKey: myPrivateKey.hex2Bin(sodium))!.toHex(sodium)
    }
    
    func verifySignature(data: String, publicKey: String) -> String? {
        sodium.sign.open(signedMessage: data.hex2Bin(sodium), publicKey: publicKey.hex2Bin(sodium))!.utf8String
    }
    
    func computeHash(data: String, type: HashType) -> String {
        let content = data.data(using: .utf8)!
        
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
        return bytes.toHex(sodium)
    }
}

extension Bytes {
    func toHex(_ sodium: Sodium) -> String {
        sodium.utils.bin2hex(self)!.uppercased()
    }
}

extension String {
    func hex2Bin(_ sodium: Sodium) -> Bytes {
        sodium.utils.hex2bin(self.lowercased())!
    }
}
