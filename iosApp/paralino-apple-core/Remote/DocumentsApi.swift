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
            let result = try await module.tablesDB.createRow(
                databaseId: databaseType.id,
                tableId: collectionId,
                rowId: docId ?? module.appConfig.resourceIds.uniqueIdPlaceholder,
                data: data
            ).parseRow()
            return NSDictionary(dictionary: result)
        }
    }
    
    func deleteDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String) async throws -> Payload<KotlinUnit> {
        await remoteModule.execute { module in
            let _ = try await module.tablesDB.deleteRow(
                databaseId: databaseType.id,
                tableId: collectionId,
                rowId: documentId
            )
            return KotlinUnit()
        }
    }
    
    func getDocument(databaseType: BackendDatabaseType, collectionId: String, documentId: String) async throws -> Payload<NSDictionary> {
        return await remoteModule.execute { module in
            let result = try await module.tablesDB.getRow(
                databaseId: databaseType.id,
                tableId: collectionId,
                rowId: documentId
            ).parseRow()
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
                
                let results = try await module.tablesDB.listRows(
                    databaseId: databaseType.id,
                    tableId: collectionId,
                    queries: mappedQueries
                )
                
                let shouldLoadMore = onNewValues(results.rows.map { try! $0.parseRow() }) == true
                if shouldLoadMore && results.rows.count >= Int(limit) {
                    cursor = results.rows.last?.id ?? ""
                } else {
                    cursor = ""
                }
            } while !cursor.isEmpty
            return KotlinUnit()
        }
    }
    
    func updateDocument(databaseType: BackendDatabaseType, collectionId: String, docId: String, data: Any) async throws -> Payload<NSDictionary> {
        return await remoteModule.execute { module in
            let result = try await module.tablesDB.updateRow(
                databaseId: databaseType.id,
                tableId: collectionId,
                rowId: docId,
                data: data
            ).parseRow()
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

private extension Appwrite.Row {
    func parseRow() throws -> [String: Any]{
        let result = try self.data.toJson()
        return SerializationExtKt.fromJsonObjectToMapOfPrimitives(result)
    }
}
