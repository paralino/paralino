package app.paralino.core.repo.api

interface IDocumentsApi {

    suspend fun createOrUpdateDocument(
        collectionId: String,
        docId: String? = null,
        data: Any,
        read: List<String>? = null,
        write: List<String>? = null
    ): Map<String, Any>

    suspend fun getDocument(collectionId: String, documentId: String): Map<String, Any>

    suspend fun getDocuments(collectionId: String): List<Map<String, Any>>
    suspend fun getDocumentsFor(collectionId: String, vararg keyValue: Pair<String, Any>): List<Map<String, Any>>

    suspend fun deleteDocument(collectionId: String, documentId: String)

}