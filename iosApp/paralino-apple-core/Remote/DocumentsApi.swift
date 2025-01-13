//
//  DocumentsApi.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import Appwrite
import shared

class DocumentsApi: IDocumentApiIOS {
    
    let remoteModule: AppwriteRemoteModule
    
    init(remoteModule: AppwriteRemoteModule) {
        self.remoteModule = remoteModule
    }
    
    func createDocument(databaseType: BackendDatabaseType, collectionId: String, docId: String?, data: Any) async throws -> Payload<NSDictionary> {
        return await remoteModule.execute { module in
            let result = try await module.database.createDocument(
                databaseId: databaseType.id,
                collectionId: collectionId,
                documentId: docId ?? module.appBuildConfig.resourceIds.uniqueIdPlaceholder,
                data: data
            ).parseDocument()
            return NSDictionary(dictionary: result)
        }
    }
    
    func deleteDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String) async throws -> Payload<KotlinUnit> {
        await remoteModule.execute { module in
            let _ = try await module.database.deleteDocument(
                databaseId: databaseType.id,
                collectionId: collectionId,
                documentId: documentId
            )
            return KotlinUnit()
        }
    }
    
    func getDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String) async throws -> Payload<NSDictionary> {
        return await remoteModule.execute { module in
            let result = try await module.database.getDocument(
                databaseId: databaseType.id,
                collectionId: collectionId,
                documentId: documentId
            ).parseDocument()
            return NSDictionary(dictionary: result)
        }
    }
    
    func listDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        queries: [any BackendQuery],
        limit: Int32,
        onNewValues: @escaping ([[String : Any]]) -> KotlinBoolean
    ) async throws -> Payload<KotlinUnit> {
        await remoteModule.execute { module in
            var cursor: String = ""
            repeat {
                var mappedQueries: [String] = []
                
                mappedQueries.append(contentsOf: self.buildQueries(queries: queries))
                mappedQueries.append(Query.limit(Int(limit)))
                if !cursor.isEmpty {
                    mappedQueries.append(Query.cursorAfter(cursor))
                }
                
                let results = try await module.database.listDocuments(
                    databaseId: databaseType.id,
                    collectionId: collectionId,
                    queries: mappedQueries
                )
                
                let shouldLoadMore = onNewValues(results.documents.map { try! $0.parseDocument() }) == true
                if shouldLoadMore && results.documents.count >= Int(limit) {
                    cursor = results.documents.last?.id ?? ""
                } else {
                    cursor = ""
                }
            } while !cursor.isEmpty
            return KotlinUnit()
        }
    }
    
    func updateDocument(databaseType: BackendDatabaseType, collectionId: String, docId: String, data: Any) async throws -> Payload<NSDictionary> {
        return await remoteModule.execute { module in
            let result = try await module.database.updateDocument(
                databaseId: databaseType.id,
                collectionId: collectionId,
                documentId: docId,
                data: data
            ).parseDocument()
            return NSDictionary(dictionary: result)
        }
    }
    
    private func buildQueries(queries: [any BackendQuery]) -> [String] {
        queries.compactMap { element in
            if let element = element as? shared.BackendQueryOR {
                return Query.or(buildQueries(queries: element.queries))
            } else if let element = element as? shared.BackendQueryEquals {
                return Query.equal(element.key, value: element.value)
            } else if let element = element as? shared.BackendQueryIsNull {
                return Query.isNull(element.key)
            }
            return nil
        }
    }
}

private extension Appwrite.Document {
    func parseDocument() throws -> [String: Any]{
        let result = try self.data.toJson()
        return SerializationExtKt.fromJsonObjectToMapOfPrimitives(result)
    }
}
