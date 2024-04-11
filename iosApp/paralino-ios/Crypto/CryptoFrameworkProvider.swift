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
    
    func execute<T>(action: () throws -> T?) -> Payload<T> {
        do {
            guard let result = try action()
            else {
                return PayloadError(exception: KotlinException(message: "Invalid value"))
            }
            return PayloadSuccess(data: result)
        } catch let error {
            Logr.i(message: "Error with crypto: \(error.localizedDescription)")
            return PayloadError(exception: KotlinException(message: error.localizedDescription))
        }
    }
    
    func createRandomId(length: Int32, alphabet: AlphabetType) -> Payload<NSString> {
        return execute {
            var randomID = ""
            for _ in 0..<length {
                randomID.append(randomCharacter(from: alphabet.alphabet))
            }
            return NSString(string: randomID)
        }
    }
    
    private func randomCharacter(from string: String) -> Character {
        let randomNum = Int(arc4random_uniform(UInt32(string.count)))
        let randomIndex = string.index(string.startIndex, offsetBy: randomNum)
        return string[randomIndex]
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
            if let value = try? sodium.box.open(
                nonceAndAuthenticatedCipherText: data.hex2Bin(sodium),
                senderPublicKey: publicKey.hex2Bin(sodium),
                recipientSecretKey: myPrivateKey.hex2Bin(sodium)
            )?.utf8String {
                NSString(string: value)
            } else {
                nil
            }
        }
    }
    
    func decryptDataWithPassword(data: String, password: String, context: String) -> Payload<NSString> {
        return execute {
            
            guard let salt = sodium.genericHash.hash(
                message: context.bytes,
                outputLength: sodium.genericHash.BytesMin
            ) else {
                throw EncryptionError.CryptoError
            }
            
            guard let key = sodium.pwHash.hash(
                outputLength: sodium.secretBox.KeyBytes,
                passwd: password.bytes,
                salt: salt,
                opsLimit: sodium.pwHash.OpsLimitInteractive,
                memLimit: sodium.pwHash.MemLimitInteractive
            ) else {
                throw EncryptionError.CryptoError
            }
            
            let result = try? decryptDataWithSymmetricKey(data: data, key: key.toHex(sodium))
            guard let result = result?.data_ as? NSString else { throw EncryptionError.CryptoError }
            return result
        }
    }
    
    func decryptDataWithSymmetricKey(data: String, key: String) -> Payload<NSString> {
        return execute {
            let result = sodium.secretBox.open(
                nonceAndAuthenticatedCipherText: try data.hex2Bin(sodium),
                secretKey: try key.hex2Bin(sodium)
            )?.utf8String
            
            if let result = result {
                return NSString(string: result)
            } else {
                Logr.i(message: "Result not valid \(result ?? "---")")
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func encryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String) -> Payload<NSString> {
        return execute {
            if let value = try? sodium.box.seal(
                message: data.bytes,
                recipientPublicKey: publicKey.hex2Bin(sodium),
                senderSecretKey: myPrivateKey.hex2Bin(sodium)
            )?.toHex(sodium) {
                NSString(string: value)
            } else {
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func encryptDataWithPassword(data: String, password: String, context: String) -> Payload<NSString> {
        return execute {
            guard let salt = sodium.genericHash.hash(
                message: context.bytes,
                outputLength: sodium.genericHash.BytesMin
            ) else {
                throw EncryptionError.CryptoError
            }
            
            guard let key = sodium.pwHash.hash(
                outputLength: sodium.secretBox.KeyBytes,
                passwd: password.bytes,
                salt: salt,
                opsLimit: sodium.pwHash.OpsLimitInteractive,
                memLimit: sodium.pwHash.MemLimitInteractive
            ) else {
                throw EncryptionError.CryptoError
            }
            
            let result = try? encryptDataWithSymmetricKey(data: data, key: key.toHex(sodium))
            guard let result = result?.data_ as? NSString else { throw EncryptionError.CryptoError }
            return result
        }
    }
    
    func encryptDataWithSymmetricKey(data: String, key: String) -> Payload<NSString> {
        return execute {
            if let value = try? sodium.secretBox.seal(
                message: data.bytes,
                secretKey: key.hex2Bin(sodium)
            )?.toHex(sodium) {
                NSString(string: value)
            } else {
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func signData(data: String, myPrivateKey: String) -> Payload<NSString> {
        return execute {
            if let value = try? sodium.sign.sign(message: data.bytes, secretKey: myPrivateKey.hex2Bin(sodium))?.toHex(sodium) {
                NSString(string: value)
            } else {
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func verifySignature(data: String, publicKey: String) -> Payload<NSString> {
        return execute {
            if let value = try? sodium.sign.open(signedMessage: data.hex2Bin(sodium), publicKey: publicKey.hex2Bin(sodium))?.utf8String {
                NSString(string: value)
            } else {
                nil
            }
        }
    }
    
    func computeHash(data: String, type: HashType) -> Payload<NSString> {
        return execute {
            guard let content = data.data(using: .utf8) else { throw EncryptionError.CryptoError }
            
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
        guard let value = sodium.utils.bin2hex(self)?.uppercased() else { throw EncryptionError.WrongFormat }
        return value
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
