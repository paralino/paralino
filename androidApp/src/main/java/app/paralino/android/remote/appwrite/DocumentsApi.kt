package app.paralino.android.remote.appwrite

import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.repo.api.IDocumentsApi
import io.appwrite.Query
import io.appwrite.models.Document
import io.appwrite.models.DocumentList

class DocumentsApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IDocumentsApi {

    override suspend fun createDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String?,
        data: Any
    ): Map<String, Any> {
        return remoteModule.execute {
            database.createDocument(
                databaseId = databaseType.id,
                collectionId = collectionId,
                documentId = docId ?: uniqueIdPlaceholder,
                data = data
            ).parseDocument()
        }
    }

    override suspend fun updateDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        docId: String,
        data: Any
    ): Map<String, Any> {
        return remoteModule.execute {
            database.updateDocument(
                databaseId = databaseType.id,
                collectionId = collectionId,
                documentId = docId,
                data = data
            ).parseDocument()
        }
    }

    override suspend fun getDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        documentId: String,
    ): Map<String, Any> {
        return remoteModule.execute {
            database.getDocument(
                databaseId = databaseType.id,
                collectionId = collectionId,
                documentId = documentId
            ).parseDocument()
        }
    }

    override suspend fun listAllDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        keyValues: List<Pair<String, Any>>,
    ): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        listDocuments(
            databaseType = databaseType,
            collectionId = collectionId,
            keyValues = keyValues,
            limit = 25,
            onNewValues = {
                result.addAll(it)
                true
            },
        )
        return result.toList()
    }

    override suspend fun listDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        keyValues: List<Pair<String, Any>>,
        limit: Int,
        onNewValues: (List<Map<String, Any>>) -> Boolean,
    ) {
        remoteModule.execute {
            var cursor: String = ""
            do {
                val results = database.listDocuments(
                    databaseId = databaseType.id,
                    collectionId = collectionId,
                    queries = buildList {
                        addAll(keyValues.map { Query.equal(attribute = it.first, value = it.second) })
                        add(Query.limit(limit))
                        if(cursor.isNotBlank())
                            add(Query.cursorAfter(cursor))
                    }
                )

                val shouldLoadMore = onNewValues(results.parseListOfDocuments())
                if(shouldLoadMore && results.documents.size >= limit) {
                    cursor = results.documents.lastOrNull()?.id ?: ""
                } else {
                    cursor = ""
                }
            } while (cursor.isNotBlank())
        }
    }

    override suspend fun deleteDocument(
        databaseType: BackendDatabaseType,
        collectionId: String,
        documentId: String,
    ) {
        remoteModule.execute {
            database.deleteDocument(
                databaseId = databaseType.id,
                collectionId = collectionId,
                documentId = documentId
            )
        }
    }

    private fun Document.parseDocument(): Map<String, Any> {
        return this.data
    }

    private fun DocumentList.parseListOfDocuments(): List<Map<String, Any>> {
        return this.documents.map { it.parseDocument() }
    }

}

