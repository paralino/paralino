package com.zbapp.remote

import com.etebase.client.ItemMetadata
import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.repo.location.ILocationRemoteDataSource
import com.zbapp.remote.user.UserRemoteSource
import kotlinx.serialization.decodeFromString

class GroupsRemoteSource(val userSource: UserRemoteSource): ILocationRemoteDataSource {

    override suspend fun getCurrentLocationsForCircle(circleId: String): List<BaseLocationModel> {
        val collection = userSource.account.collectionManager.list(Constants.collectionTypeGroups).data.firstOrNull {
            it.uid == circleId
        } ?: return emptyList()

        return when(collection.meta.itemType) {
            Constants.collectionGroupUser -> {
                userSource.account.collectionManager.getItemManager(collection).list().data
                    .filter { it.meta.itemType == Constants.collectionGroupItemCurrent }
                    .map { BaseRemoteModule.formatter.decodeFromString(it.contentString) }
                    ?: emptyList()
            }
            Constants.collectionGroupPublic -> {
                listOf(BaseRemoteModule.formatter.decodeFromString(collection.contentString))
            }
            Constants.collectionGroupPrivate -> {
                listOf(BaseRemoteModule.formatter.decodeFromString(collection.contentString))
            }
            else -> emptyList()
        }
    }

    override suspend fun getLocationsHistoryForUser(circleId: String, userId: String): List<BaseLocationModel> {
        val collection = userSource.account.collectionManager.list(Constants.collectionTypeGroups).data.firstOrNull {
            it.uid == circleId || it.meta.itemType == circleId
        } ?: return emptyList()

        return when(collection.meta.itemType) {
            Constants.collectionGroupUser -> {
                userSource.account.collectionManager.getItemManager(collection).list().data
                    .firstOrNull {
                        it.meta.itemType == Constants.collectionGroupItemHistory &&
                                it.meta.description == userId
                    }
                    ?.let {
                        BaseRemoteModule.formatter.decodeFromString<List<BaseLocationModel>>(it.contentString)
                    } ?: emptyList()
            }
            Constants.collectionGroupPublic -> {
                listOf(BaseRemoteModule.formatter.decodeFromString<BaseLocationModel>(collection.contentString))
            }
            Constants.collectionGroupPrivate -> {
                emptyList()
            }
            else -> emptyList()
        }
    }

    override suspend fun getMyLocationsHistory(date: String): List<BaseLocationModel> {
        return emptyList()
    }

    override suspend fun setLocations(current: List<BaseLocationModel>, history: List<BaseLocationModel>) {
        val collectionIds = current.map { it.circleId } + history.map { it.circleId }
        val collections = userSource.account.collectionManager.list(Constants.collectionTypeGroups).data.filter {
            collectionIds.contains(it.uid)
        }

        current.forEach { item ->
            val collection = collections.firstOrNull { it.uid == item.circleId }
            val items = BaseRemoteModule.formatter.decodeFromString<List<BaseRemoteModule>>(collection?.contentString ?: "")

        }

    }

    suspend fun initGroupsCollection() {
        userSource.account.collectionManager.create(
            Constants.collectionTypeGroups,
            ItemMetadata().apply {
                itemType = Constants.collectionGroupPublic
                name = "Public sharing"
            },
            ""
        )

        userSource.account.collectionManager.create(
            Constants.collectionTypeGroups,
            ItemMetadata().apply {
                itemType = Constants.collectionGroupPrivate
                name = "Personal log"
            },
            ""
        )
    }

}