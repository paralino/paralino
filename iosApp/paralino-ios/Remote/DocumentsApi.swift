//
//  DocumentsApi.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import Appwrite
import shared

class DocumentsApi: IDocumentsApi {
    
    let remoteModule: AppwriteRemoteModule
    
    init(remoteModule: AppwriteRemoteModule) {
        self.remoteModule = remoteModule
    }
    
    override func createDocument(databaseType: BackendDatabaseType, collectionId: String, docId: String?, data: Any) async throws -> [String : Any] {
        return await remoteModule.execute { module in
            try! await module.database.createDocument(
                databaseId: databaseType.id,
                collectionId: collectionId,
                documentId: docId ?? module.backendConfig.resourceIds.uniqueIdPlaceholder,
                data: data
            ).parseDocument()
        }
    }
    
    override func deleteDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String) async throws {
        await remoteModule.execute { module in
            try! await module.database.deleteDocument(
                databaseId: databaseType.id,
                collectionId: collectionId,
                documentId: documentId
            )
        }
    }
    
    override func getDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String) async throws -> [String : Any] {
        return await remoteModule.execute { module in
            try! await module.database.getDocument(
                databaseId: databaseType.id,
                collectionId: collectionId,
                documentId: documentId
            ).parseDocument()
        }
    }
    
    override func listDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        keyValues: [KotlinPair<NSString, AnyObject>],
        limit: Int32,
        onNewValues: @escaping ([[String : Any]]) -> KotlinBoolean
    ) async throws {
        await remoteModule.execute { module in
            var cursor: String = ""
            repeat {
                var queries: [String] = []
                keyValues.forEach { element in
                    queries.append(Query.equal(element.first! as String, value: element.second!))
                }
                queries.append(Query.limit(Int(limit)))
                if !cursor.isEmpty {
                    queries.append(Query.cursorAfter(cursor))
                }
                
                let results = try! await module.database.listDocuments(
                    databaseId: databaseType.id,
                    collectionId: collectionId,
                    queries: queries
                )
                
                let shouldLoadMore = onNewValues(results.documents.map { $0.parseDocument() }) == true
                if shouldLoadMore && results.documents.count >= Int(limit) {
                    cursor = results.documents.last?.id ?? ""
                } else {
                    cursor = ""
                }
            } while !cursor.isEmpty
        }
    }
    
    override func updateDocument(databaseType: BackendDatabaseType, collectionId: String, docId: String, data: Any) async throws -> [String : Any] {
        return await remoteModule.execute { module in
            try! await module.database.updateDocument(
                databaseId: databaseType.id,
                collectionId: collectionId,
                documentId: docId,
                data: data
            ).parseDocument()
        }
    }
}

private extension Appwrite.Document {
    func parseDocument() -> [String: Any] {
        let result = try! self.data.toJson()
        return SerializationExtKt.fromJsonObjectToMapOfPrimitives(result)
    }
}
