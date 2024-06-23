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
            NSString(string: try sodium.secretBox.key().encodeKey(self))
        }
    }
    
    func createNewCryptoKeyPair() -> Payload<KeyPair> {
        return execute {
            let keys = sodium.box.keyPair()
            return KeyPair(
                privateKey: try keys!.secretKey.encodeKey(self),
                publicKey: try keys!.publicKey.encodeKey(self)
            )
        }
    }
    
    func createNewSignatureKeyPair() -> Payload<KeyPair> {
        return execute {
            let keys = sodium.sign.keyPair()
            return KeyPair(
                privateKey: try keys!.secretKey.encodeKey(self),
                publicKey: try keys!.publicKey.encodeKey(self)
            )
        }
    }
    
    func deriveKeyFromPassword(password: String, salt: String, alg: KDFAlg) async throws -> Payload<NSString> {
        return execute {
            
            switch alg.kdfType {
            case .argon2id:
                guard let saltBytes = sodium.genericHash.hash(
                    message: salt.bytes,
                    outputLength: sodium.genericHash.BytesMin
                ) else {
                    throw EncryptionError.CryptoError
                }
                
                guard let key = sodium.pwHash.hash(
                    outputLength: Int(alg.keyBytes),
                    passwd: password.bytes,
                    salt: saltBytes,
                    opsLimit: Int(alg.rounds),
                    memLimit: Int(alg.memoryBytes),
                    alg: PWHash.Alg.Argon2ID13
                ) else {
                    throw EncryptionError.CryptoError
                }
                
                return NSString(string: try key.encodeKey(self))
            default: throw EncryptionError.CryptoError
            }
        }
    }
    
    func deriveLongSymmetricKey(key: String, derivedKeyId: Int64, context: String) -> Payload<NSString> {
        return execute {
            guard let newKey = sodium.keyDerivation.derive(
                secretKey: try key.decodeKey(self),
                index: UInt64(derivedKeyId),
                length: sodium.keyDerivation.BytesMax,
                context: context
            ) else {
                throw EncryptionError.CryptoError
            }
            return NSString(string: try newKey.encodeKey(self))
        }
    }
    
    func encryptDataWithSymmetricKey(data: String, key: String) -> Payload<NSString> {
        return execute {
            if let value = try? sodium.secretBox.seal(
                message: data.bytes,
                secretKey: key.decodeKey(self)
            )?.encodeWithCipherAlg(self) {
                NSString(string: value)
            } else {
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func decryptDataWithSymmetricKey(data: String, key: String) -> Payload<NSString> {
        return execute {
            
            let cipher = try data.decodeCipher(self)
            
            switch cipher.algorithm {
            case .xsalsa20poly1305, .default_:
                let result = sodium.secretBox.open(
                    nonceAndAuthenticatedCipherText: try cipher.decode(self),
                    secretKey: try key.decodeKey(self)
                )?.utf8String
                
                if let result = result {
                    return NSString(string: result)
                } else {
                    Logr.i(message: "Result not valid \(result ?? "---")")
                    throw EncryptionError.CryptoError
                }
            default:
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func encryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String) -> Payload<NSString> {
        return execute {
            if let value = try? sodium.box.seal(
                message: data.bytes,
                recipientPublicKey: publicKey.decodeKey(self),
                senderSecretKey: myPrivateKey.decodeKey(self)
            )?.encodeWithCipherAlg(self) {
                NSString(string: value)
            } else {
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func decryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String) -> Payload<NSString> {
        return execute {
            
            let cipher = try data.decodeCipher(self)
            
            switch cipher.algorithm {
            case .xsalsa20poly1305, .default_:
                let result = try? sodium.box.open(
                    nonceAndAuthenticatedCipherText: cipher.decode(self),
                    senderPublicKey: publicKey.decodeKey(self),
                    recipientSecretKey: myPrivateKey.decodeKey(self)
                )?.utf8String
                
                if let result = result {
                    return NSString(string: result)
                } else {
                    throw EncryptionError.CryptoError
                }
            default:
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func signData(data: String, myPrivateKey: String) -> Payload<NSString> {
        return execute {
            if let value = try? sodium.sign.sign(
                message: data.bytes,
                secretKey: myPrivateKey.decodeKey(self)
            )?.encodeWithCipherAlg(self) {
                NSString(string: value)
            } else {
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func verifySignature(data: String, publicKey: String) -> Payload<NSString> {
        return execute {
            let cipher = try data.decodeCipher(self)
            
            switch cipher.algorithm {
            case .xsalsa20poly1305, .default_:
                if let value = try? sodium.sign.open(
                    signedMessage: cipher.decode(self),
                    publicKey: publicKey.decodeKey(self)
                )?.utf8String {
                    return NSString(string: value)
                } else {
                    throw EncryptionError.CryptoError
                }
            default:
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func computeHash(data: String, type: HashType, encodingType: EncodingType) -> Payload<NSString> {
        return execute {
            guard let content = data.data(using: .utf8) else { throw EncryptionError.CryptoError }
            
            let hash = sodium.genericHash.hash(message: data.bytes, key: nil, outputLength: Int(type.bytes))
            
            var result: String?
            switch encodingType {
            case .hex: result = try hash?.encodeToHex(self)
            case .base64: result = try hash?.encodeToBase64(self)
            case .base64Url: result = try hash?.encodeToBase64Url(self)
            default: throw EncryptionError.WrongFormat
            }
            
            if let result = result {
                return NSString(string: result)
            } else {
                throw EncryptionError.CryptoError
            }
        }
    }
}

extension Bytes {
    //    func encode(_ framework: CryptoFrameworkProvider) throws -> String {
    //        switch framework.cryptoScheme {
    //        case ..<2: return try self.encodeToHex(framework)
    //        default: return try self.encodeToBase64(framework)
    //        }
    //    }
    
    func encodeKey(_ framework: CryptoFrameworkProvider) throws -> String {
        try self.encodeToBase64Url(framework)
    }
    
    func encodeWithCipherAlg(_ framework: CryptoFrameworkProvider) throws -> String {
        CipherAlg(encodingType: .base64Url, algType: .xsalsa20poly1305, data: try self.encodeToBase64Url(framework)).encode()
    }
    
    func encodeToBase64(_ framework: CryptoFrameworkProvider) throws -> String {
        guard let value = framework.sodium.utils.bin2base64(self, variant: .ORIGINAL) else { throw EncryptionError.WrongFormat }
        return value
    }
    
    func encodeToBase64Url(_ framework: CryptoFrameworkProvider) throws -> String {
        guard let value = framework.sodium.utils.bin2base64(self, variant: .URLSAFE_NO_PADDING) else { throw EncryptionError.WrongFormat }
        return value
    }
    
    func encodeToHex(_ framework: CryptoFrameworkProvider) throws -> String {
        guard let value = framework.sodium.utils.bin2hex(self)?.uppercased() else { throw EncryptionError.WrongFormat }
        return value
    }
}

extension String {
    
    //    func decode(_ framework: CryptoFrameworkProvider) throws -> Bytes {
    //        switch framework.cryptoScheme {
    //        case ..<2: return try self.decodeFromHex(framework)
    //        default: return try self.decodeFromBase64(framework)
    //        }
    //    }
    
    func decodeKey(_ framework: CryptoFrameworkProvider) throws -> Bytes {
        try self.decodeFromBase64Url(framework)
    }
    
    func decodeCipher(_ framework: CryptoFrameworkProvider) throws -> CipherAlg {
        CipherAlg.companion.decode(input: self)
    }
    
    func decodeFromBase64(_ framework: CryptoFrameworkProvider) throws -> Bytes {
        if let bytes = framework.sodium.utils.base642bin(self, variant: .ORIGINAL) {
            return bytes
        } else {
            throw EncryptionError.WrongFormat
        }
    }
    
    func decodeFromBase64Url(_ framework: CryptoFrameworkProvider) throws -> Bytes {
        if let bytes = framework.sodium.utils.base642bin(self, variant: .URLSAFE_NO_PADDING) {
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

extension CipherAlg {
    func decode(_ framework: CryptoFrameworkProvider) throws -> Bytes {
        switch self.encodingType {
        case .hex: return try self.data.decodeFromHex(framework)
        case .base64: return try self.data.decodeFromBase64(framework)
        case .base64Url: return try self.data.decodeFromBase64Url(framework)
        default: throw EncryptionError.WrongFormat
        }
    }
}

enum EncryptionError: Error {
    case WrongFormat
    case CryptoError
}
