//
//  CryptoFrameworkProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 07.04.23.
//

import Foundation
import shared
import Sodium

class CryptoFrameworkProvider: ICryptoServiceProvider {
    
    let sodium: Sodium
    
    init(sodium: Sodium) {
        self.sodium = sodium
    }
    
    init() {
        sodium = Sodium()
    }
    
    func createNewAsymmetricKey() -> KeyPair {
        let keys = sodium.sign.keyPair()
        return KeyPair(
            privateKey: keys!.secretKey.toSharedData().content,
            publicKey: keys!.secretKey.toSharedData().content
        )
    }
    
    func createNewSymmetricKey() -> shared.Data {
        return sodium.secretBox.key().toSharedData()
    }
    
    func decryptDataWithKeyPair(data: KotlinByteArray, myPrivateKey: KotlinByteArray, publicKey: KotlinByteArray) -> shared.Data {
        sodium.box.open(
            nonceAndAuthenticatedCipherText: data.toBytes(),
            senderPublicKey: publicKey.toBytes(),
            recipientSecretKey: myPrivateKey.toBytes()
        )!.toSharedData()
    }
    
    func decryptDataWithPassword(data: KotlinByteArray, password: String, context: KotlinByteArray) -> shared.Data {
        let salt = sodium.genericHash.hash(
            message: context.toBytes(),
            outputLength: sodium.genericHash.BytesMin
        )

        let key = sodium.pwHash.hash(
            outputLength: sodium.pwHash.StrBytes,
            passwd: password.bytes,
            salt: salt!,
            opsLimit: sodium.pwHash.OpsLimitInteractive,
            memLimit: sodium.pwHash.MemLimitInteractive
        )
        
        return decryptDataWithSymmetricKey(data: data, key: key!.toSharedData().content)
    }
    
    func decryptDataWithSymmetricKey(data: KotlinByteArray, key: KotlinByteArray) -> shared.Data {
        sodium.secretBox.open(
            nonceAndAuthenticatedCipherText: data.toBytes(),
            secretKey: key.toBytes()
        )!.toSharedData()
    }
    
    func encryptDataWithKeyPair(data: KotlinByteArray, myPrivateKey: KotlinByteArray, publicKey: KotlinByteArray) -> shared.Data {
        sodium.box.seal(
            message: data.toBytes(),
            recipientPublicKey: publicKey.toBytes(),
            senderSecretKey: myPrivateKey.toBytes()
        )!.toSharedData()
    }
    
    func encryptDataWithPassword(data: KotlinByteArray, password: String, context: KotlinByteArray) -> shared.Data {
        
        let salt = sodium.genericHash.hash(
            message: context.toBytes(),
            outputLength: sodium.genericHash.BytesMin
        )

        let key = sodium.pwHash.hash(
            outputLength: sodium.pwHash.StrBytes,
            passwd: password.bytes,
            salt: salt!,
            opsLimit: sodium.pwHash.OpsLimitInteractive,
            memLimit: sodium.pwHash.MemLimitInteractive
        )
        
        return encryptDataWithSymmetricKey(data: data, key: key!.toSharedData().content)
    }
    
    func encryptDataWithSymmetricKey(data: KotlinByteArray, key: KotlinByteArray) -> shared.Data {
        sodium.secretBox.seal(
            message: data.toBytes(),
            secretKey: key.toBytes()
        )!.toSharedData()
    }
    
    func signData(data: KotlinByteArray, myPrivateKey: KotlinByteArray) -> shared.Data {
        sodium.sign.sign(message: data.toBytes(), secretKey: myPrivateKey.toBytes())!.toSharedData()
    }
    
    func verifySignature(data: KotlinByteArray, publicKey: KotlinByteArray) -> shared.Data? {
        sodium.sign.open(signedMessage: data.toBytes(), publicKey: publicKey.toBytes())!.toSharedData()
    }
}
