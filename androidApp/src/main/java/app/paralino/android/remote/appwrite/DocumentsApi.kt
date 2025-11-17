package app.paralino.android.remote.appwrite

import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.repo.api.BackendQuery
import app.paralino.core.repo.api.IDocumentsApi
import io.appwrite.Query
import io.appwrite.models.Row
import io.appwrite.models.RowList

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
            tablesDB.createRow(
                databaseId = databaseType.id,
                tableId = collectionId,
                rowId = docId ?: this.appConfig.resourceIds.uniqueIdPlaceholder,
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
            tablesDB.updateRow(
                databaseId = databaseType.id,
                tableId = collectionId,
                rowId = docId,
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
            tablesDB.getRow(
                databaseId = databaseType.id,
                tableId = collectionId,
                rowId = documentId
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
                val results = tablesDB.listRows(
                    databaseId = databaseType.id,
                    tableId = collectionId,
                    queries = buildList {
                        addAll(buildQuery(queries))
                        add(Query.limit(limit))
                        if(cursor.isNotBlank())
                            add(Query.cursorAfter(cursor))
                    }
                )

                val shouldLoadMore = onNewValues(results.parseListOfDocuments())
                if(shouldLoadMore && results.rows.size >= limit) {
                    cursor = results.rows.lastOrNull()?.id ?: ""
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
            tablesDB.deleteRow(
                databaseId = databaseType.id,
                tableId = collectionId,
                rowId = documentId
            )
        }
    }

    private fun Row<Map<String, Any>>.parseDocument(): Map<String, Any> {
        return this.data
    }

    private fun RowList<Map<String, Any>>.parseListOfDocuments(): List<Map<String, Any>> {
        return this.rows.map { it.parseDocument() }
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

