//
//  CryptoFrameworkProviderTests.swift
//  paralino-ios
//
//  Created by Zelimir on 2026-04-24.
//


import XCTest
import shared
@testable import paralino_apple_core

final class CryptoFrameworkProviderTests: XCTestCase {

    private func asError(_ throwable: KotlinThrowable) -> Error {
        let message = throwable.message ?? String(describing: throwable)
        return NSError(domain: "app.paralino.core", code: 1, userInfo: [NSLocalizedDescriptionKey: message])
    }

    private func unwrapString(_ payload: Any?) throws -> String {
        let p = PayloadSwift<NSString>(data: payload)
        switch p {
        case .success(let data):
            return data as String
        case .error(let error):
            throw asError(error)
        case .loading:
            throw NSError(domain: "app.paralino.core", code: 2, userInfo: [NSLocalizedDescriptionKey: "Unexpected loading payload"])
        }
    }

    private func unwrapKeyPair(_ payload: Any?) throws -> SimpleKeyPairData {
        let p = PayloadSwift<SimpleKeyPairData>(data: payload)
        switch p {
        case .success(let data):
            return data
        case .error(let error):
            throw asError(error)
        case .loading:
            throw NSError(domain: "app.paralino.core", code: 2, userInfo: [NSLocalizedDescriptionKey: "Unexpected loading payload"])
        }
    }

    func testSymmetricEncryptDecrypt_roundTrip() throws {
        let crypto = CryptoFrameworkProvider()
        let key = try unwrapString(crypto.createNewSymmetricKey())
        let plain = "hello"

        let cipher = try unwrapString(crypto.encryptDataWithSymmetricKey(data: plain, key: key))
        let decoded = try unwrapString(crypto.decryptDataWithSymmetricKey(data: cipher, key: key))

        XCTAssertEqual(plain, decoded)
    }

    func testSymmetricEncryptDecrypt_defaultKeyDerive_hasCipherAlgPrefix() throws {
        let crypto = CryptoFrameworkProvider()
        let key = try unwrapString(crypto.createNewSymmetricKeyForDerivation())
        let plain = "hello"

        let cipher = try unwrapString(crypto.encryptDataWithSymmetricKey(data: plain, key: key))
        XCTAssertTrue(cipher.hasPrefix("@:"), "Expected tagged CipherAlg output, got: \(cipher)")
        XCTAssertTrue(cipher.contains(":@"), "Expected tagged CipherAlg delimiter, got: \(cipher)")

        let decoded = try unwrapString(crypto.decryptDataWithSymmetricKey(data: cipher, key: key))
        XCTAssertEqual(plain, decoded)
    }

    func testSymmetricEncryptDecrypt_defaultKey_omitsCipherAlgPrefix() throws {
        let crypto = CryptoFrameworkProvider()
        let key = try unwrapString(crypto.createNewSymmetricKey())
        let plain = "hello"

        let cipher = try unwrapString(crypto.encryptDataWithSymmetricKey(data: plain, key: key))
        XCTAssertFalse(cipher.hasPrefix("@:"), "Expected DEFAULT cipher to omit tags, got: \(cipher)")

        let decoded = try unwrapString(crypto.decryptDataWithSymmetricKey(data: cipher, key: key))
        XCTAssertEqual(plain, decoded)
    }

    func testKeyPairEncryptDecrypt_roundTripBetweenTwoParties() throws {
        let crypto = CryptoFrameworkProvider()
        let alice = try unwrapKeyPair(crypto.createNewCryptoKeyPair())
        let bob = try unwrapKeyPair(crypto.createNewCryptoKeyPair())

        let plain = "secret"
        let cipher = try unwrapString(crypto.encryptDataWithKeyPair(data: plain, myPrivateKey: alice.privateKey, publicKey: bob.publicKey))
        let decoded = try unwrapString(crypto.decryptDataWithKeyPair(data: cipher, myPrivateKey: bob.privateKey, publicKey: alice.publicKey))

        XCTAssertEqual(plain, decoded)
    }

    func testSignVerify_roundTrip() throws {
        let crypto = CryptoFrameworkProvider()
        let kp = try unwrapKeyPair(crypto.createNewSignatureKeyPair())
        let plain = "payload"

        let signed = try unwrapString(crypto.signData(data: plain, myPrivateKey: kp.privateKey))
        let verified = try unwrapString(crypto.verifySignature(data: signed, publicKey: kp.publicKey))

        XCTAssertEqual(plain, verified)
    }

    func testVerifySignature_returnsErrorPayload_forTamperedSignature() throws {
        let crypto = CryptoFrameworkProvider()
        let kp = try unwrapKeyPair(crypto.createNewSignatureKeyPair())
        let plain = "payload"

        let signed = try unwrapString(crypto.signData(data: plain, myPrivateKey: kp.privateKey))
        let tampered = signed + "x"

        let p = PayloadSwift<NSString>(data: crypto.verifySignature(data: tampered, publicKey: kp.publicKey))
        switch p {
        case .success:
            XCTFail("Expected verifySignature to fail for tampered signature")
        case .error:
            break
        case .loading:
            XCTFail("Unexpected loading payload")
        }
    }

    func testDeriveKeyFromPassword_isDeterministic() async throws {
        let crypto = CryptoFrameworkProvider()
        let alg = KDFAlg(
            encoding: EncodingType.base64Url.key,
            kdf: KDFType.argon2id.key,
            memoryMb: 32,
            rounds: 2,
            keyBytes: 32
        )

        let d1 = try unwrapString(await crypto.deriveKeyFromPassword(password: "pw", salt: "salt", alg: alg))
        let d2 = try unwrapString(await crypto.deriveKeyFromPassword(password: "pw", salt: "salt", alg: alg))
        let d3 = try unwrapString(await crypto.deriveKeyFromPassword(password: "pw", salt: "salt2", alg: alg))

        XCTAssertEqual(d1, d2)
        XCTAssertNotEqual(d1, d3)
    }

    func testVerifyUserKeysChange_v1_roundTripAndFailure() async throws {
        let provider = CryptoFrameworkProvider()
        let serviceProvider = CryptoServiceWrapperIOS(cryptoIOS: provider)
        let cryptoCore = CryptoCore(serviceProvider: serviceProvider)
        let op = CryptoAccountOperationV1(cryptoCore: cryptoCore)

        let userId = "user_1"
        let password = "pw_old"
        let alg = cryptoCore.getDefaultLoginKDFAlg()

        let authKey = try await op.deriveUserAuthKey(salt: "salt", password: password, alg: alg)
        let signatureKeyPair = serviceProvider.createNewSignatureKeyPair()

        let keys = try await op.changeUserMasterKey(
            userId: userId,
            signatureKeyPair: signatureKeyPair,
            authKey: authKey
        )

        // should pass
        try await op.verifyUserKeysChange(
            oldKey: keys.encryptedKeyData,
            oldMasterAuthKey: authKey.masterAuthKey,
            oldMasterKey: keys.userPersonalKey
        )

        // should fail
        do {
            try await op.verifyUserKeysChange(
                oldKey: keys.encryptedKeyData,
                oldMasterAuthKey: authKey.masterAuthKey,
                oldMasterKey: keys.userPersonalKey + "_wrong"
            )
            XCTFail("Expected verifyUserKeysChange to throw")
        } catch {
            // message comes from Kotlin check() - keep it loose
            XCTAssertTrue(String(describing: error).contains("Old key does not match"))
        }
    }

    func testDeriveSymmetricKey_isDeterministicForSameInputs() throws {
        let crypto = CryptoFrameworkProvider()
        let masterKey = try unwrapString(crypto.createNewSymmetricKey())

        let k1 = try unwrapString(crypto.deriveSymmetricKey(key: masterKey, derivedKeyId: 1, context: "prlndata"))
        let k2 = try unwrapString(crypto.deriveSymmetricKey(key: masterKey, derivedKeyId: 1, context: "prlndata"))
        let k3 = try unwrapString(crypto.deriveSymmetricKey(key: masterKey, derivedKeyId: 2, context: "prlndata"))

        XCTAssertEqual(k1, k2)
        XCTAssertNotEqual(k1, k3)
    }

    func testComputeHash_isDeterministic() throws {
        let crypto = CryptoFrameworkProvider()
        let h1 = try unwrapString(crypto.computeHash(data: "data", type: .generic256, encodingType: .base64Url))
        let h2 = try unwrapString(crypto.computeHash(data: "data", type: .generic256, encodingType: .base64Url))
        XCTAssertEqual(h1, h2)
    }

    func testCreateRandomId_hasExpectedLength_andAlphabet() throws {
        let crypto = CryptoFrameworkProvider()
        let id = try unwrapString(crypto.createRandomId(length: 16, alphabet: .safe))
        XCTAssertEqual(16, id.count)

        let safeAlphabet = AlphabetType.safe.alphabet
        XCTAssertTrue(id.allSatisfy { safeAlphabet.contains($0) })
    }
}

