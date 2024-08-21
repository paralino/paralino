package app.paralino.android.remote.appwrite

import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.repo.api.BackendQuery
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
                documentId = docId ?: this.appBuildConfig.resourceIds.uniqueIdPlaceholder,
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

    override suspend fun listDocuments(
        databaseType: BackendDatabaseType,
        collectionId: String,
        queries: List<BackendQuery>,
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
                        addAll(buildQuery(queries))
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

    private fun Document<Map<String, Any>>.parseDocument(): Map<String, Any> {
        return this.data
    }

    private fun DocumentList<Map<String, Any>>.parseListOfDocuments(): List<Map<String, Any>> {
        return this.documents.map { it.parseDocument() }
    }

    private fun buildQuery(queries: List<BackendQuery>): List<String> {
        return queries.map { query ->
            when (query) {
                is BackendQuery.OR -> {
                    Query.or(buildQuery(query.queries))
                }
                is BackendQuery.Equals -> {
                    Query.equal(attribute = query.key, value = query.value)
                }

                is BackendQuery.IsNull -> Query.isNull(attribute = query.key)
            }
        }
    }
}

