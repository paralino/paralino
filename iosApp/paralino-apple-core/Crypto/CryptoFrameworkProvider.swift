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

    init() {
        self.sodium = Sodium()
    }
    
    func execute<T>(action: () throws -> T?) -> Payload<T> {
        do {
            guard let result = try action()
            else {
                return PayloadError(exception: KotlinException(message: "Invalid value"))
            }
            return PayloadSuccess(data: result)
        } catch let error {
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
            NSString(string: try sodium.aead.xchacha20poly1305ietf.key().encodeWithCipherAlg(self, alg: .default_))
        }
    }
    
    func createNewSymmetricKeyForDerivation() -> Payload<NSString> {
        return execute {
            NSString(string: try sodium.aead.xchacha20poly1305ietf.key().encodeWithCipherAlg(self, alg: .defaultKeyDerive))
        }
    }
    
    func createNewCryptoKeyPair() -> Payload<SimpleKeyPairData> {
        return execute {
            let keys = sodium.box.keyPair()
            return SimpleKeyPairData(
                privateKey: try keys!.secretKey.encodeWithCipherAlg(self),
                publicKey: try keys!.publicKey.encodeWithCipherAlg(self)
            )
        }
    }
    
    func createNewSignatureKeyPair() -> Payload<SimpleKeyPairData> {
        return execute {
            let keys = sodium.sign.keyPair()
            return SimpleKeyPairData(
                privateKey: try keys!.secretKey.encodeWithCipherAlg(self),
                publicKey: try keys!.publicKey.encodeWithCipherAlg(self)
            )
        }
    }
    
    func deriveKeyFromPassword(password: String, salt: String, alg: KDFAlg) async throws -> Payload<NSString> {
        return execute {
            switch alg.kdfType {
            case .argon2id:
                guard let saltBytes = sodium.genericHash.hash(
                    message: salt.bytes,
                    outputLength: sodium.pwHash.SaltBytes
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
                
                var result: String?
                switch alg.encodingType {
                case .hex: result = try key.encodeToHex(self)
                case .base64: result = try key.encodeToBase64(self)
                case .base64Url: result = try key.encodeToBase64Url(self)
                default: throw EncryptionError.WrongFormat
                }
                
                if let result = result {
                    return NSString(string: result)
                } else {
                    throw EncryptionError.CryptoError
                }
            default: throw EncryptionError.CryptoError
            }
        }
    }
    
    func deriveSymmetricKey(key: String, derivedKeyId: Int64, context: String) -> Payload<NSString> {
        return execute {
            return NSString(string: try deriveSymmetricKeyImpl(key: key, derivedKeyId: derivedKeyId, context: context))
        }
    }
    
    func deriveSymmetricKeyImpl(key: String, derivedKeyId: Int64, context: String) throws -> String {
        let decodedKey = try key.decodeCipher(self)
        switch decodedKey.algorithm {
        case .default_:
            guard let newKey = sodium.keyDerivation.derive(
                secretKey: try decodedKey.decodeData(self),
                index: UInt64(derivedKeyId),
                length: sodium.keyDerivation.KeyBytes,
                context: context
            ) else {
                throw EncryptionError.CryptoError
            }
            return try newKey.encodeWithCipherAlg(self, alg: .default_)
        default: throw EncryptionError.WrongFormat
        }
    }
    
    func encryptDataWithSymmetricKey(data: String, key: String) -> Payload<NSString> {
        return execute {
            let decodedKey = try key.decodeCipher(self)
            switch decodedKey.algorithm {
            case .default_:
                if let value = try? sodium.aead.xchacha20poly1305ietf.encrypt(
                    message: data.bytes,
                    secretKey: try decodedKey.decodeData(self)
                )?.encodeWithCipherAlg(self, alg: .default_) {
                    return NSString(string: value)
                } else {
                    throw EncryptionError.CryptoError
                }
            case .defaultKeyDerive:
                let keyId = sodium.randomBytes.random()
                let newKey = try deriveSymmetricKeyImpl(
                    key: decodedKey.data,
                    derivedKeyId: Int64(keyId),
                    context: "_derive_"
                )
                if let value = try? sodium.aead.xchacha20poly1305ietf.encrypt(
                    message: data.bytes,
                    secretKey: try newKey.decodeCipher(self).decodeData(self)
                )?.encodeWithCipherAlg(
                    self,
                    alg: .defaultKeyDerive,
                    payload: "\(keyId)".bytes.encodeToBase64Url(self)
                ) {
                    return NSString(string: value)
                } else {
                    throw EncryptionError.CryptoError
                }
            default:
                throw EncryptionError.CryptoError
            }
        }
    }
    
    func decryptDataWithSymmetricKey(data: String, key: String) -> Payload<NSString> {
        return execute {
            let cipher = try data.decodeCipher(self)
            let decodedKey = try key.decodeCipher(self)
            
            if cipher.algorithm != decodedKey.algorithm {
                throw EncryptionError.WrongFormat
            }
            
            switch cipher.algorithm {
            case .default_:
                let result = sodium.aead.xchacha20poly1305ietf.decrypt(
                    nonceAndAuthenticatedCipherText: try cipher.decodeData(self),
                    secretKey: try decodedKey.decodeData(self)
                )?.utf8String
                if let result = result {
                    return NSString(string: result)
                } else {
                    Logr.i(message: "Result not valid \(result ?? "---")")
                    throw EncryptionError.CryptoError
                }
            case .defaultKeyDerive:
                
                guard let keyIdPayload = try cipher.payload?.decodeFromBase64Url(self).utf8String else {
                    throw EncryptionError.CryptoError
                }
                guard let keyId = Int64(keyIdPayload) else {
                    throw EncryptionError.CryptoError
                }
                
                let newKey = try deriveSymmetricKeyImpl(
                    key: decodedKey.data,
                    derivedKeyId: keyId,
                    context: "_derive_"
                )

                let result = sodium.aead.xchacha20poly1305ietf.decrypt(
                    nonceAndAuthenticatedCipherText: try cipher.decodeData(self),
                    secretKey: try newKey.decodeCipher(self).decodeData(self)
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
            let decodedPrivateKey = try myPrivateKey.decodeCipher(self)
            let decodedPublicKey = try publicKey.decodeCipher(self)
            
            if decodedPrivateKey.algorithm != decodedPublicKey.algorithm {
                throw EncryptionError.WrongFormat
            }
            
            switch decodedPublicKey.algorithm {
            case .default_:
                if let value = try? sodium.box.seal(
                    message: data.bytes,
                    recipientPublicKey: decodedPublicKey.decodeData(self),
                    senderSecretKey: decodedPrivateKey.decodeData(self)
                )?.encodeWithCipherAlg(self, alg: .default_) {
                    return NSString(string: value)
                } else {
                    throw EncryptionError.CryptoError
                }
            default: throw EncryptionError.CryptoError
            }
        }
    }
    
    func decryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String) -> Payload<NSString> {
        return execute {
            let cipher = try data.decodeCipher(self)
            let decodedPrivateKey = try myPrivateKey.decodeCipher(self)
            let decodedPublicKey = try publicKey.decodeCipher(self)
            
            if decodedPrivateKey.algorithm != decodedPublicKey.algorithm {
                throw EncryptionError.WrongFormat
            }
            
            if cipher.algorithm != decodedPublicKey.algorithm {
                throw EncryptionError.WrongFormat
            }
            
            switch cipher.algorithm {
            case .default_:
                let result = try? sodium.box.open(
                    nonceAndAuthenticatedCipherText: cipher.decodeData(self),
                    senderPublicKey: decodedPublicKey.decodeData(self),
                    recipientSecretKey: decodedPrivateKey.decodeData(self)
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
            let decodedPrivateKey = try myPrivateKey.decodeCipher(self)
            switch decodedPrivateKey.algorithm {
            case .default_:
                if let value = try? sodium.sign.sign(
                    message: data.bytes,
                    secretKey: decodedPrivateKey.decodeData(self)
                )?.encodeWithCipherAlg(self, alg: .default_) {
                    return NSString(string: value)
                } else {
                    throw EncryptionError.CryptoError
                }
            default: throw EncryptionError.WrongFormat
            }
        }
    }
    
    func verifySignature(data: String, publicKey: String) -> Payload<NSString> {
        return execute {
            let cipher = try data.decodeCipher(self)
            let decodedPublicKey = try publicKey.decodeCipher(self)
            
            if cipher.algorithm != decodedPublicKey.algorithm {
                throw EncryptionError.WrongFormat
            }
            
            switch cipher.algorithm {
            case .default_:
                if let value = try? sodium.sign.open(
                    signedMessage: cipher.decodeData(self),
                    publicKey: decodedPublicKey.decodeData(self)
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
    func encodeWithCipherAlg(_ framework: CryptoFrameworkProvider, alg: CipherAlgType = .default_, payload: String? = nil) throws -> String {
        CipherAlg.init(encodingType: .base64Url, algType: alg, payload: payload, data: try self.encodeToBase64Url(framework)).encode(skipTagsIfDefault: true)
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
    func decodeData(_ framework: CryptoFrameworkProvider) throws -> Bytes {
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
