package app.paralino.core.domain.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.ILimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.mappers.PersonalCodeMapperCore
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.IGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.*

class GroupCreateUseCase(
    private val userRepository: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val personalCodesRepo: PersonalCodesRepository,
    private val personalCodeMapper: PersonalCodeMapperCore,
    private val remote: GroupsRemoteSource,
    private val local: IGroupLocalDataSource
): BaseRepository(), ILimitedExecution<String> {

    override val executionState = MutableStateFlow(listOf<String>())
    private val groupKey = "group_create"

    fun createGroup(name: String, image: ImageModel): Flow<Unit> {

        if (isProcessing(groupKey)) {
            logg.tag("group_actions").v("Processing, can't create new group $groupKey")
            return flow { throw IllegalStateException("Group creation already in progress") }
        }

        setAsProcessing(groupKey)
        return personalCodesRepo.createPersonalCode()
            .map { personalCodeMapper.mapMyCode(it) }
            .flatMapMerge { code ->
                getDataOnce(
                    remote = {
                        val userData = userRepository.cachedUser!!.data!!
                        remote.createGroup(
                            name = name,
                            image = image,
                            code = code,
                            myData = MemberModelData(userData.userName, userData.userImage),
                            myKey = userKeyRepo.latestKeyPair()
                        )
                            .also { cryptoKEKRepo.refreshKeysForGroup(it.group.id).first() }
                            .also { stopProcessing(groupKey) }
                    },
                    localUpdate = { local.updateOrCreateGroup(it) }
                ).map { Unit }
            }
            .catch {
                stopProcessing(groupKey)
                throw it
            }
    }


}