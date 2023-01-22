package app.paralino.core.domain.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.ILimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.IGroupLocalDataSource
import kotlinx.coroutines.flow.*

class GroupMemberAddUseCase constructor(
    private val local: IGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
): BaseRepository(), ILimitedExecution<String> {

    override val executionState = MutableStateFlow(listOf<String>())

    fun addMemberByAccessCode(groupId: String, code: UserPersonalCodeData): Flow<GroupDocumentRaw> {

        if (isProcessing(groupId)) {
            logg.tag("group_actions").v("Processing, can't add new member $groupId")
            return flow { throw IllegalStateException("Group member creation already in progress") }
        }

        return getDataOnce(
            remote = {
                setAsProcessing(groupId)
                val group = local.getGroupWithMembers(groupId).first()
                remote.addUserToGroup(
                    group,
                    code,
                    userKeyRepo.latestKeyPair()
                ).also {
                    cryptoKEKRepo.refreshKeysForGroup(groupId).first()
                }.also {
                    stopProcessing(groupId)
                }
            },
            localUpdate = {
                local.updateOrCreateGroup(it)
            }
        )
            .map { it.group }
            .catch {
                stopProcessing(groupId)
                throw it
            }
    }

}