package com.zbapp.remote.api

import com.etebase.client.Collection
import com.etebase.client.FetchOptions
import com.etebase.client.ItemMetadata
import com.etebase.client.exceptions.NotFoundException
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.repo.group.IGroupRemoteDataSource
import com.zbapp.core.repo.location.ILocationRemoteDataSource
import com.zbapp.remote.BaseRemoteModule
import com.zbapp.remote.Constants
import com.zbapp.remote.blocking
import com.zbapp.remote.user.RemoteUserCache
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal class GroupsApi (
    private val userSource: RemoteUserCache
): ILocationRemoteDataSource, IGroupRemoteDataSource {

    override suspend fun getCurrentLocationsForGroup(groupId: String): List<BaseLocationModel> {

        val collection = blocking { userSource.account.collectionManager.fetch(groupId) }

        return when(collection.meta.itemType) {
            Constants.collectionGroupUser -> {
                blocking {
                    userSource.account.collectionManager.getItemManager(collection).list().data
                        .filter { it.meta.itemType == Constants.collectionGroupItemCurrent }
                        .map { BaseRemoteModule.formatter.decodeFromString(it.contentString) }
                        ?: emptyList()
                }
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

    override suspend fun getLocationsHistoryForUser(groupId: String, userId: String): List<BaseLocationModel> {
        val collection = userSource.account.collectionManager.fetch(groupId)

        return when(collection.meta.itemType) {
            Constants.collectionGroupUser -> {
                blocking {
                    userSource.account.collectionManager.getItemManager(collection).list().data
                        .firstOrNull {
                            it.meta.itemType == Constants.collectionGroupItemHistory &&
                                    it.meta.description == userId
                        }
                        ?.let {
                            BaseRemoteModule.formatter.decodeFromString<List<BaseLocationModel>>(it.contentString)
                        } ?: emptyList()
                }
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
        val collectionIds = current.map { it.groupId } + history.map { it.groupId }
        val collections = blocking {
            userSource.account.collectionManager.list(Constants.collectionTypeGroups).data.filter {
                collectionIds.contains(it.uid)
            }
        }

        current.forEach { item ->
            val collection = collections.firstOrNull { it.uid == item.groupId }
            val items = BaseRemoteModule.formatter.decodeFromString<List<BaseRemoteModule>>(collection?.contentString ?: "")

        }

    }

    override suspend fun initGroups() {
        createGroup(UserGroup.empty(), Constants.collectionGroupPrivate, "Personal log")
        createGroup(UserGroup.empty(), Constants.collectionGroupPublic, "Public sharing")
    }

    private suspend fun createGroup(group: UserGroup, type: String = Constants.collectionGroupUser, infoName: String = ""): Collection {
        return blocking {
            val collection = userSource.account.collectionManager.create(
                Constants.collectionTypeGroups,
                ItemMetadata().apply {
                    itemType = type
                    name = infoName
                },
                BaseRemoteModule.formatter.encodeToString(group)
            )
            userSource.account.collectionManager.upload(collection)
            collection
        }
    }

    override suspend fun createOrUpdateGroup(group: UserGroup): UserGroup {
        if(group.id.isBlank()) {
            return createGroup(group).let { group.copy(id = it.uid) }
        } else {
            return blocking {
                try {
                    val collection = userSource.account.collectionManager.fetch(group.id)
                    collection.setContent(BaseRemoteModule.formatter.encodeToString(group))
                    userSource.account.collectionManager.upload(collection)
                    group
                } catch (e: Exception) {
                    if(e is NotFoundException) {
                        createGroup(group).let { group.copy(id = it.uid) }
                    } else throw e
                }
            }
        }
    }

    override suspend fun getAllGroups(): List<UserGroup> {
        return blocking {
            userSource.account.collectionManager.list(Constants.collectionTypeGroups).data
                .map { collection ->
                    BaseRemoteModule.formatter.decodeFromString<UserGroup>(collection.contentString)
                        .copy(id = collection.uid)
                }
        }
    }

    override suspend fun getGroup(groupId: String): UserGroup {
        return blocking {
            userSource.account.collectionManager.fetch(groupId).contentString.let {
                BaseRemoteModule.formatter.decodeFromString(it)
            }
        }
    }

    override suspend fun deleteGroup(groupId: String) {
        blocking {
            userSource.account.collectionManager.fetch(groupId).delete()
        }
    }
}