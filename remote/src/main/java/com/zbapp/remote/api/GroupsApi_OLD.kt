package com.zbapp.remote.api

internal class GroupsApi_OLD (
) {
//
//    override suspend fun getCurrentLocationsForGroup(groupId: String): List<BaseLocationModel> {
//
//        val collection = blocking { userSource.account.collectionManager.fetch(groupId) }
//
//        return when(collection.meta.itemType) {
//            Constants.collectionGroupUser -> {
//                blocking {
//                    userSource.account.collectionManager.getItemManager(collection).list().data
//                        .filter { it.meta.itemType == Constants.collectionGroupItemCurrent }
//                        .map { BaseRemoteModule.formatter.decodeFromString(it.contentString) }
//                        ?: emptyList()
//                }
//            }
//            Constants.collectionGroupPublic -> {
//                listOf(BaseRemoteModule.formatter.decodeFromString(collection.contentString))
//            }
//            Constants.collectionGroupPrivate -> {
//                listOf(BaseRemoteModule.formatter.decodeFromString(collection.contentString))
//            }
//            else -> emptyList()
//        }
//    }
//
//    override suspend fun getLocationsHistoryForUser(groupId: String, userId: String): List<BaseLocationModel> {
//        val collection = userSource.account.collectionManager.fetch(groupId)
//
//        return when(collection.meta.itemType) {
//            Constants.collectionGroupUser -> {
//                blocking {
//                    userSource.account.collectionManager.getItemManager(collection).list().data
//                        .firstOrNull {
//                            it.meta.itemType == Constants.collectionGroupItemHistory &&
//                                    it.meta.description == userId
//                        }
//                        ?.let {
//                            BaseRemoteModule.formatter.decodeFromString<List<BaseLocationModel>>(it.contentString)
//                        } ?: emptyList()
//                }
//            }
//            Constants.collectionGroupPublic -> {
//                listOf(BaseRemoteModule.formatter.decodeFromString<BaseLocationModel>(collection.contentString))
//            }
//            Constants.collectionGroupPrivate -> {
//                emptyList()
//            }
//            else -> emptyList()
//        }
//    }
//
//    override suspend fun getMyLocationsHistory(date: String): List<BaseLocationModel> {
//        return emptyList()
//    }
//
//    override suspend fun setLocations(current: List<BaseLocationModel>, history: List<BaseLocationModel>) {
//        val collectionIds = current.map { it.groupId } + history.map { it.groupId }
//        val collections = blocking {
//            userSource.account.collectionManager.list(Constants.collectionTypeGroups).data.filter {
//                collectionIds.contains(it.uid)
//            }
//        }
//
//        current.forEach { item ->
//            val collection = collections.firstOrNull { it.uid == item.groupId }
//            val items = BaseRemoteModule.formatter.decodeFromString<List<BaseRemoteModule>>(collection?.contentString ?: "")
//
//        }
//
//    }
//
//    override suspend fun initGroups() {
//        createGroup(UserGroup.personalTracking)
//        createGroup(UserGroup.publicSharing)
//    }
//
//    private suspend fun createGroup(group: UserGroup, infoName: String = ""): Collection {
//        return blocking {
//            val collection = userSource.account.collectionManager.create(
//                Constants.collectionTypeGroups,
//                ItemMetadata().apply {
//                    itemType = group.type
//                    name = infoName.takeIf { it.isNotBlank() } ?: group.name
//                },
//                BaseRemoteModule.formatter.encodeToString(group)
//            )
//            userSource.account.collectionManager.upload(collection)
//            collection
//        }
//    }
//
//    override suspend fun createOrUpdateGroup(group: UserGroup): UserGroup {
//        if(group.id.isBlank()) {
//            return createGroup(group).let { group.copy(id = it.uid) }
//        } else {
//            return blocking {
//                try {
//                    val collection = userSource.account.collectionManager.fetch(group.id)
//                    collection.setContent(BaseRemoteModule.formatter.encodeToString(group))
//                    userSource.account.collectionManager.upload(collection)
//                    group
//                } catch (e: Exception) {
//                    if(e is NotFoundException) {
//                        createGroup(group).let { group.copy(id = it.uid) }
//                    } else throw e
//                }
//            }
//        }
//    }
//
//    override suspend fun getAllGroups(): List<UserGroup> {
//        return blocking {
//            userSource.account.collectionManager.list(Constants.collectionTypeGroups).data
//                .map { collection ->
//                    if(collection.contentString.isBlank() || collection.isDeleted) {
//                        null
//                    } else {
//                        BaseRemoteModule.formatter.decodeFromString<UserGroup>(collection.contentString)
//                            .copy(id = collection.uid)
//                    }
//                }.filterNotNull()
//        }
//    }
//
//    override suspend fun getGroup(groupId: String): UserGroup {
//        return blocking {
//            userSource.account.collectionManager.fetch(groupId).contentString.let {
//                BaseRemoteModule.formatter.decodeFromString<UserGroup>(it).copy(id = groupId)
//            }
//        }
//    }
//
//    override suspend fun deleteGroup(groupId: String) {
//        blocking {
//            val collection = userSource.account.collectionManager.fetch(groupId)
//            collection.delete()
//            userSource.account.collectionManager.upload(collection)
//        }
//    }
}