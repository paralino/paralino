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

class CryptoFrameworkProvider: ICryptoServiceProviderIOS {
    
    let sodium: Sodium
    
    init(sodium: Sodium) {
        self.sodium = sodium
    }
    
    init() {
        sodium = Sodium()
    }
    
    func execute<T>(action: () throws -> T) -> Payload<T> {
        do {
            let result = try action()
            return PayloadSuccess(data: result)
        } catch let error {
            Logr.i(message: "Error with crypto: \(error.localizedDescription)")
            return PayloadError(exception: KotlinThrowable(message: error.localizedDescription))
        }
    }
    
    func execute2<T>(action: () async throws -> T) async -> Payload<T> {
        do {
            let result = try await action()
            return PayloadSuccess(data: result)
        } catch let error {
            Logr.i(message: "Error with crypto: \(error.localizedDescription)")
            return PayloadError<T>(exception: KotlinThrowable(message: "message"))
        }
    }
    
    func createNewSymmetricKey() -> Payload<NSString> {
        return execute {
            NSString(string: try sodium.secretBox.key().toHex(sodium))
        }
    }
    
    func createNewCryptoKeyPair() -> Payload<KeyPair> {
        return execute {
            let keys = sodium.box.keyPair()
            return KeyPair(
                privateKey: try keys!.secretKey.toHex(sodium),
                publicKey: try keys!.publicKey.toHex(sodium)
            )
        }
    }
    
    func createNewSignatureKeyPair() -> Payload<KeyPair> {
        return execute {
            let keys = sodium.sign.keyPair()
            return KeyPair(
                privateKey: try keys!.secretKey.toHex(sodium),
                publicKey: try keys!.publicKey.toHex(sodium)
            )
        }
    }
    
    func decryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String) -> Payload<NSString> {
        return execute {
            NSString(
                string: try sodium.box.open(
                    nonceAndAuthenticatedCipherText: data.hex2Bin(sodium),
                    senderPublicKey: publicKey.hex2Bin(sodium),
                    recipientSecretKey: myPrivateKey.hex2Bin(sodium)
                )!.utf8String!
            )
        }
    }
    
    func decryptDataWithPassword(data: String, password: String, context: String) async throws -> Payload<NSString> {
        return await execute2 {
            
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
            let result = try await decryptDataWithSymmetricKey(data: data, key: key!.toHex(sodium)).data_ as! NSString
            return result
        }
    }
    
    func decryptDataWithSymmetricKey(data: String, key: String) async throws -> Payload<NSString> {
        return await execute2 {
            let result = sodium.secretBox.open(
                nonceAndAuthenticatedCipherText: try data.hex2Bin(sodium),
                secretKey: try key.hex2Bin(sodium)
            )?.utf8String
            
            if let result = result {
                Logr.i(message: "Result valid - \(result)")
                return NSString(string: result)
            } else {
                Logr.i(message: "Result not valid \(result ?? "---")")
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func encryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String) -> Payload<NSString> {
        return execute {
            NSString(
                string: try sodium.box.seal(
                    message: data.bytes,
                    recipientPublicKey: publicKey.hex2Bin(sodium),
                    senderSecretKey: myPrivateKey.hex2Bin(sodium)
                )!.toHex(sodium)
            )
        }
    }
    
    func encryptDataWithPassword(data: String, password: String, context: String) -> Payload<NSString> {
        return execute {
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
            
            let result = try encryptDataWithSymmetricKey(data: data, key: key!.toHex(sodium)).data_ as! NSString
            return result
        }
    }
    
    func encryptDataWithSymmetricKey(data: String, key: String) -> Payload<NSString> {
        return execute {
            NSString(
                string: try sodium.secretBox.seal(
                    message: data.bytes,
                    secretKey: key.hex2Bin(sodium)
                )!.toHex(sodium)
            )
        }
    }
    
    func signData(data: String, myPrivateKey: String) -> Payload<NSString> {
        return execute {
            NSString(string: try sodium.sign.sign(message: data.bytes, secretKey: myPrivateKey.hex2Bin(sodium))!.toHex(sodium))
        }
    }
    
    func verifySignature(data: String, publicKey: String) -> Payload<NSString> {
        return execute {
            NSString(string: try sodium.sign.open(signedMessage: data.hex2Bin(sodium), publicKey: publicKey.hex2Bin(sodium))!.utf8String!)
        }
    }
    
    func computeHash(data: String, type: HashType) -> Payload<NSString> {
        return execute {
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
            return NSString(string: try bytes.toHex(sodium))
        }
    }
}

extension Bytes {
    func toHex(_ sodium: Sodium) throws -> String {
        sodium.utils.bin2hex(self)!.uppercased()
    }
}

extension String {
    func hex2Bin(_ sodium: Sodium) throws -> Bytes {
        if let bytes = sodium.utils.hex2bin(self.lowercased()) {
            return bytes
        } else {
            throw EncryptionError.WrongFormat
        }
    }
}

enum EncryptionError: Error {
    case WrongFormat
    case CryptoError
}
