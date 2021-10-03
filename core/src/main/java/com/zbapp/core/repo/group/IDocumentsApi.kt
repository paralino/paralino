package com.zbapp.core.repo.group

import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface IDocumentsApi {
    suspend fun createOrUpdateDocument(
        collectionId: String,
        data: SimpleDocumentRaw,
        users: List<String>? = null
    ): DocumentResponse

    suspend fun createOrUpdateDocument(
        collectionId: String,
        groupId: String,
        data: Map<String, String>,
        users: List<String>? = null
    ): DocumentResponse

    suspend fun getDocument(collectionId: String, groupId: String): DocumentResponse

    suspend fun getDocuments(collectionId: String): DocumentsResponse

    suspend fun deleteDocument(collectionId: String, groupId: String)

    companion object {
        fun getDocumentFromJson(content: DocumentResponse): SimpleDocumentRaw {
            val json = BaseRemoteModule.formatter.parseToJsonElement(content)
            return parseDocument(json)
        }

        fun getDocumentsFromJson(content: DocumentsResponse): List<SimpleDocumentRaw> {
            val json = BaseRemoteModule.formatter.parseToJsonElement(content)
            return json.jsonObject["documents"]?.jsonArray?.map { parseDocument(it) } ?: emptyList()
        }

        private fun parseDocument(element: JsonElement): SimpleDocumentRaw {
            return SimpleDocumentRaw(
                id = element.jsonObject["id"]!!.jsonPrimitive.content,
                ownerId = "",
                data = element.jsonObject["data"]?.jsonObject?.jsonPrimitive?.content ?: ""
            )
        }
    }
}

typealias DocumentsResponse = String
typealias DocumentResponse = String