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
    let cryptoScheme: Int32
    
    init(cryptoScheme: Int32) {
        self.sodium = Sodium()
        self.cryptoScheme = cryptoScheme
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
            NSString(string: try sodium.secretBox.key().encode(self))
        }
    }
    
    func createNewCryptoKeyPair() -> Payload<KeyPair> {
        return execute {
            let keys = sodium.box.keyPair()
            return KeyPair(
                privateKey: try keys!.secretKey.encode(self),
                publicKey: try keys!.publicKey.encode(self)
            )
        }
    }
    
    func createNewSignatureKeyPair() -> Payload<KeyPair> {
        return execute {
            let keys = sodium.sign.keyPair()
            return KeyPair(
                privateKey: try keys!.secretKey.encode(self),
                publicKey: try keys!.publicKey.encode(self)
            )
        }
    }
    
    func decryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String) -> Payload<NSString> {
        return execute {
            if let value = try? sodium.box.open(
                nonceAndAuthenticatedCipherText: data.decode(self),
                senderPublicKey: publicKey.decode(self),
                recipientSecretKey: myPrivateKey.decode(self)
            )?.utf8String {
                NSString(string: value)
            } else {
                nil
            }
        }
    }
    
    func decryptDataWithPassword(data: String, password: String, context: String, useFastMode: Bool) -> Payload<NSString> {
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
                opsLimit: Int(PasswordHashingDefaults.shared.getHashingIterations(useFastMode: useFastMode, cryptoScheme: cryptoScheme)),
                memLimit: Int(PasswordHashingDefaults.shared.getHashingMemoryInBytes(useFastMode: useFastMode, cryptoScheme: cryptoScheme)),
                alg: PWHash.Alg.Argon2ID13
            ) else {
                throw EncryptionError.CryptoError
            }
            
            let result = try? decryptDataWithSymmetricKey(data: data, key: key.encode(self))
            guard let result = result?.data_ as? NSString else { throw EncryptionError.CryptoError }
            return result
        }
    }
    
    func decryptDataWithSymmetricKey(data: String, key: String) -> Payload<NSString> {
        return execute {
            let result = sodium.secretBox.open(
                nonceAndAuthenticatedCipherText: try data.decode(self),
                secretKey: try key.decode(self)
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
                recipientPublicKey: publicKey.decode(self),
                senderSecretKey: myPrivateKey.decode(self)
            )?.encode(self) {
                NSString(string: value)
            } else {
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func encryptDataWithPassword(data: String, password: String, context: String, useFastMode: Bool) -> Payload<NSString> {
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
                opsLimit: Int(PasswordHashingDefaults.shared.getHashingIterations(useFastMode: useFastMode, cryptoScheme: cryptoScheme)),
                memLimit: Int(PasswordHashingDefaults.shared.getHashingMemoryInBytes(useFastMode: useFastMode, cryptoScheme: cryptoScheme)),
                alg: PWHash.Alg.Argon2ID13
            ) else {
                throw EncryptionError.CryptoError
            }
            
            let result = try? encryptDataWithSymmetricKey(data: data, key: key.encode(self))
            guard let result = result?.data_ as? NSString else { throw EncryptionError.CryptoError }
            return result
        }
    }
    
    func encryptDataWithSymmetricKey(data: String, key: String) -> Payload<NSString> {
        return execute {
            if let value = try? sodium.secretBox.seal(
                message: data.bytes,
                secretKey: key.decode(self)
            )?.encode(self) {
                NSString(string: value)
            } else {
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func signData(data: String, myPrivateKey: String) -> Payload<NSString> {
        return execute {
            if let value = try? sodium.sign.sign(message: data.bytes, secretKey: myPrivateKey.decode(self))?.encode(self) {
                NSString(string: value)
            } else {
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func verifySignature(data: String, publicKey: String) -> Payload<NSString> {
        return execute {
            if let value = try? sodium.sign.open(signedMessage: data.decode(self), publicKey: publicKey.decode(self))?.utf8String {
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
            return NSString(string: try bytes.encodeToHex(self))
        }
    }
}

extension Bytes {
    func encode(_ framework: CryptoFrameworkProvider) throws -> String {
        switch framework.cryptoScheme {
        case ..<2: return try self.encodeToHex(framework)
        default: return try self.encodeToBase64(framework)
        }
    }
    
    func encodeToBase64(_ framework: CryptoFrameworkProvider) throws -> String {
        guard let value = framework.sodium.utils.bin2base64(self, variant: .ORIGINAL) else { throw EncryptionError.WrongFormat }
        return value
    }
    
    func encodeToHex(_ framework: CryptoFrameworkProvider) throws -> String {
        guard let value = framework.sodium.utils.bin2hex(self)?.uppercased() else { throw EncryptionError.WrongFormat }
        return value
    }
}

extension String {
    func decode(_ framework: CryptoFrameworkProvider) throws -> Bytes {
        switch framework.cryptoScheme {
        case ..<2: return try self.decodeFromHex(framework)
        default: return try self.decodeFromBase64(framework)
        }
    }
    
    func decodeFromBase64(_ framework: CryptoFrameworkProvider) throws -> Bytes {
        if let bytes = framework.sodium.utils.base642bin(self, variant: .ORIGINAL) {
            return bytes
        } else {
            throw EncryptionError.WrongFormat
        }
    }
    
    func decodeFromHex(_ framework: CryptoFrameworkProvider) throws -> Bytes {
        if let bytes = framework.sodium.utils.hex2bin(self.lowercased()) {
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
