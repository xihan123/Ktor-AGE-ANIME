package tk.xihantest.dao

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import tk.xihantest.dao.UsersDatabaseFactory.dbQuery
import tk.xihantest.models.*

import tk.xihantest.models.Users.chaseAnimeData
import tk.xihantest.models.Users.created_at
import tk.xihantest.models.Users.favoriteData
import tk.xihantest.models.Users.historyData
import tk.xihantest.plugins.json
import tk.xihantest.utils.Utils
import tk.xihantest.utils.sync.Config.defaultPortraitUrl
import tk.xihantest.utils.sync.entity.AnimeInfosEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DAOFacadeImpl : DAOFacade {

    private fun resultRowToUser(row: ResultRow) = User(
        id = row[Users.id],
        name = row[Users.name],
        pass = row[Users.pass],
        email = row[Users.email],
        portraitUrl = row[Users.portraitUrl],
        favoriteData = json.decodeFromString(row[favoriteData]),
        historyData = json.decodeFromString(row[historyData]),
        chaseAnimeData = json.decodeFromString(row[chaseAnimeData]),
        created_at = row[created_at]
    )

    override suspend fun allUsers(): List<User> = dbQuery {
        Users.selectAll().map(::resultRowToUser)
    }

    override suspend fun user(id: Int): User? = dbQuery {
        Users.select { Users.id eq id }.mapNotNull(::resultRowToUser).singleOrNull()
    }

    override suspend fun userFavorite(id: Int): User.FavoriteDataDTO? = dbQuery {
        Users.select { Users.id eq id }.mapNotNull(::resultRowToUser).singleOrNull()?.favoriteData
    }

    override suspend fun userHistory(id: Int): User.HistoryDataDTO? = dbQuery {
        Users.select { Users.id eq id }.mapNotNull(::resultRowToUser).singleOrNull()?.historyData
    }

    override suspend fun userChaseAnime(id: Int): User.ChaseAnimeDataDTO? = dbQuery {
        Users.select { Users.id eq id }.mapNotNull(::resultRowToUser).singleOrNull()?.chaseAnimeData
    }

    override suspend fun addNewUser(userEntity: User): User? = dbQuery {
        Users.insert {
            it[name] = userEntity.name
            it[pass] = userEntity.pass
            it[email] = userEntity.email
            it[portraitUrl] = userEntity.portraitUrl
            it[favoriteData] = json.encodeToString(userEntity.favoriteData)
            it[historyData] = json.encodeToString(userEntity.historyData)
            it[chaseAnimeData] = json.encodeToString(userEntity.chaseAnimeData)
            it[created_at] = userEntity.created_at
        }.resultedValues?.singleOrNull()?.let(::resultRowToUser)
    }

    override suspend fun updateUser(id: Int, newPass: String?, newEmail: String?, newPortraitUrl: String?): Boolean =
        dbQuery {
            try {
                Users.update({ Users.id eq id }) {
                    if (newPass != null) {
                        it[pass] = newPass
                    }
                    if (newEmail != null) {
                        it[email] = newEmail
                    }
                    if (newPortraitUrl != null) {
                        it[portraitUrl] = newPortraitUrl
                    }
                } > 0
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun addNewFavourite(
        userId: Int,
        newFavourite: MutableList<User.FavoriteDataDTO.DataListDTO>,
    ): Boolean = dbQuery {
        try {
            userFavorite(userId)?.list?.let { favoriteList ->
                favoriteList.addAll(newFavourite)
                Users.update({ Users.id eq userId }) {
                    it[favoriteData] =
                        json.encodeToString(User.FavoriteDataDTO(list = favoriteList, allSize = favoriteList.size))
                } > 0
            } ?: run {
                Users.update({ Users.id eq userId }) {
                    it[favoriteData] =
                        json.encodeToString(User.FavoriteDataDTO(list = newFavourite, allSize = newFavourite.size))
                } > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addNewHistory(
        userId: Int,
        newHistoryList: MutableList<User.HistoryDataDTO.DataListDTO>,
    ): Boolean = dbQuery {
        try {
            userHistory(userId)?.list?.let { historyList ->
                historyList.addAll(newHistoryList)
                Users.update({ Users.id eq userId }) {
                    it[historyData] =
                        json.encodeToString(User.HistoryDataDTO(list = historyList, allSize = historyList.size))
                } > 0
            } ?: run {
                Users.update({ Users.id eq userId }) {
                    it[historyData] =
                        json.encodeToString(User.HistoryDataDTO(list = newHistoryList, allSize = newHistoryList.size))
                } > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addNewChaseAnime(
        userId: Int,
        newChaseAnimeList: MutableList<User.ChaseAnimeDataDTO.DataListDTO>,
    ): Boolean = dbQuery {
        try {
            userChaseAnime(userId)?.list?.let { chaseAnimeList ->
                chaseAnimeList.addAll(newChaseAnimeList)
                Users.update({ Users.id eq userId }) {
                    it[chaseAnimeData] = json.encodeToString(User.ChaseAnimeDataDTO(list = chaseAnimeList,
                        allSize = chaseAnimeList.size))
                } > 0
            } ?: run {
                Users.update({ Users.id eq userId }) {
                    it[chaseAnimeData] = json.encodeToString(User.ChaseAnimeDataDTO(list = newChaseAnimeList,
                        allSize = newChaseAnimeList.size))
                } > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateChaseAnime(
        userId: Int,
        newChaseAnimeList: MutableList<User.ChaseAnimeDataDTO.DataListDTO>,
    ): Boolean = dbQuery {
        try {
            Users.update({ Users.id eq userId }) {
                it[chaseAnimeData] = json.encodeToString(User.ChaseAnimeDataDTO(list = newChaseAnimeList,
                    allSize = newChaseAnimeList.size))
            } > 0
        } catch (e: Exception) {
            false
        }

    }

    override suspend fun updateHistory(
        userId: Int,
        newHistoryList: MutableList<User.HistoryDataDTO.DataListDTO>,
    ): Boolean = dbQuery {
        try {
            val userHistoryList = userHistory(userId)?.list
            newHistoryList.forEach {  newHistoryListIt ->
                userHistoryList?.find {it.f_AID == newHistoryListIt.f_AID }?.let { dataList ->
                    dataList.f_TITLE = newHistoryListIt.f_TITLE
                    dataList.f_IMG_URL = newHistoryListIt.f_IMG_URL
                    dataList.f_PLAY_URL = newHistoryListIt.f_PLAY_URL
                    dataList.f_PLAY_NUMBER = newHistoryListIt.f_PLAY_NUMBER
                    dataList.f_UPDATE_TIME = newHistoryListIt.f_UPDATE_TIME
                    dataList.f_PLAYER_NUMBER = newHistoryListIt.f_PLAYER_NUMBER
                    dataList.f_PROGRESS = newHistoryListIt.f_PROGRESS
                    dataList.f_DURATION = newHistoryListIt.f_DURATION
                    dataList.f_PLAYER_LIST = newHistoryListIt.f_PLAYER_LIST
                }?:addNewHistory(userId, newHistoryList)
            }
            if (!userHistoryList.isNullOrEmpty()){
                Users.update({ Users.id eq userId }) {
                    it[historyData] =
                        json.encodeToString(User.HistoryDataDTO(list = userHistoryList, allSize = userHistoryList.size))
                } > 0
            }else{
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeChaseAnime(userId: Int, animeId: String, animeIds: List<String>): Boolean = dbQuery {
        try {
            userChaseAnime(userId)?.list?.let { list ->
                if (!Utils.isNull(animeId)) {
                    list.removeAll { it.f_AID == animeId }
                } else if (animeIds.isNotEmpty()) {
                    list.removeAll { animeIds.contains(it.f_AID) }
                }
                Users.update({ Users.id eq userId }) {
                    it[chaseAnimeData] = json.encodeToString(User.ChaseAnimeDataDTO(list = list, allSize = list.size))
                } > 0
            } ?: run {
                Users.update({ Users.id eq userId }) {
                    it[chaseAnimeData] =
                        json.encodeToString(User.ChaseAnimeDataDTO(list = mutableListOf(), allSize = 0))
                } > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeHistory(userId: Int, animeId: String, animeIds: List<String>): Boolean = dbQuery {
        try {
            userHistory(userId)?.list?.let { list ->
                if (!Utils.isNull(animeId)) {
                    list.removeAll { it.f_AID == animeId }
                } else if (animeIds.isNotEmpty()) {
                    list.removeAll { animeIds.contains(it.f_AID) }
                }
                Users.update({ Users.id eq userId }) {
                    it[historyData] = json.encodeToString(User.HistoryDataDTO(list = list, allSize = list.size))
                } > 0
            } ?: run {
                Users.update({ Users.id eq userId }) {
                    it[historyData] = json.encodeToString(User.HistoryDataDTO(list = mutableListOf(), allSize = 0))
                } > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeFavourite(userId: Int, animeId: String, animeIds: List<String>): Boolean = dbQuery {
        try {
            userFavorite(userId)?.list?.let { favoriteList ->
                if (!Utils.isNull(animeId)) {
                    favoriteList.removeAll { it.f_AID == animeId }
                } else if (animeIds.isNotEmpty()) {
                    favoriteList.removeAll { animeIds.contains(it.f_AID) }
                }
                Users.update({ Users.id eq userId }) {
                    it[favoriteData] =
                        json.encodeToString(User.FavoriteDataDTO(list = favoriteList, allSize = favoriteList.size))
                } > 0
            } ?: run {
                Users.update({ Users.id eq userId }) {
                    it[favoriteData] = json.encodeToString(User.FavoriteDataDTO(list = mutableListOf(), allSize = 0))
                } > 0
            }
        } catch (e: Exception) {
            false
        }

    }

    override suspend fun removeUser(id: Int): Boolean = dbQuery {
        try {
            Users.deleteWhere { Users.id eq id } > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun resultRowToSimpleAnimeData(row: ResultRow) = SimpleAnimeData(
        animeId = row[SimpleAnimeDatas.animeId],
        r???????????? = row[SimpleAnimeDatas.r????????????],
        r???????????? = row[SimpleAnimeDatas.r????????????],
        r???????????? = json.decodeFromString(row[SimpleAnimeDatas.r????????????]),
        r???????????? = row[SimpleAnimeDatas.r????????????],
        r???????????? = row[SimpleAnimeDatas.r????????????],
        r?????? = row[SimpleAnimeDatas.r??????],
        r???????????? = row[SimpleAnimeDatas.r????????????],
        r???????????? = row[SimpleAnimeDatas.r????????????],
        r???????????? = row[SimpleAnimeDatas.r????????????],
        r???????????? = row[SimpleAnimeDatas.r????????????],
        r?????? = row[SimpleAnimeDatas.r??????],
        r???????????? = row[SimpleAnimeDatas.r????????????]
    )

    override suspend fun allSimpleAnimeData(): List<SimpleAnimeData> = dbQuery {
        SimpleAnimeDatas.selectAll().map(::resultRowToSimpleAnimeData)
    }

    override suspend fun simpleAnimeData(aid: String): SimpleAnimeData? = dbQuery {
        SimpleAnimeDatas.select { SimpleAnimeDatas.animeId eq aid }.map(::resultRowToSimpleAnimeData).firstOrNull()
    }

    override suspend fun addNewSimpleAnimeData(simpleAnimeData: SimpleAnimeData): SimpleAnimeData? = dbQuery {
        SimpleAnimeDatas.insert {
            it[animeId] = simpleAnimeData.animeId
            it[r????????????] = simpleAnimeData.r????????????
            it[r????????????] = simpleAnimeData.r????????????
            it[r????????????] = json.encodeToString(simpleAnimeData.r????????????)
            it[r????????????] = simpleAnimeData.r????????????
            it[r????????????] = simpleAnimeData.r????????????
            it[r??????] = simpleAnimeData.r??????
            it[r????????????] = simpleAnimeData.r????????????
            it[r????????????] = simpleAnimeData.r????????????
            it[r????????????] = simpleAnimeData.r????????????
            it[r????????????] = simpleAnimeData.r????????????
            it[r??????] = simpleAnimeData.r??????
            it[r????????????] = simpleAnimeData.r????????????
        }.resultedValues?.singleOrNull()?.let(::resultRowToSimpleAnimeData)
    }

    override suspend fun updateSimpleAnimeData(
        aid: String,
        simpleAnimeData: SimpleAnimeData
    ): Boolean = dbQuery {
        try {
            SimpleAnimeDatas.update({ SimpleAnimeDatas.animeId eq aid }) {
                it[r????????????] = simpleAnimeData.r????????????
                it[r????????????] = simpleAnimeData.r????????????
                it[r????????????] = json.encodeToString(simpleAnimeData.r????????????)
                it[r????????????] = simpleAnimeData.r????????????
                it[r????????????] = simpleAnimeData.r????????????
                it[r??????] = simpleAnimeData.r??????
                it[r????????????] = simpleAnimeData.r????????????
                it[r????????????] = simpleAnimeData.r????????????
                it[r????????????] = simpleAnimeData.r????????????
                it[r????????????] = simpleAnimeData.r????????????
                it[r??????] = simpleAnimeData.r??????
                it[r????????????] = simpleAnimeData.r????????????
            } > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeSimpleAnimeData(aid: String): Boolean = dbQuery {
        SimpleAnimeDatas.deleteWhere { SimpleAnimeDatas.animeId eq aid } > 0
    }

    private fun resultRowToCompleteAnimeData(row: ResultRow) = CompleteAnimeData(
        aID = row[CompleteAnimeDatas.animeId],
        collectCnt = row[CompleteAnimeDatas.collectCnt].toInt(),
        commentCnt = row[CompleteAnimeDatas.commentCnt].toInt(),
        dEFPLAYINDEX = row[CompleteAnimeDatas.dEFPLAYINDEX],
        filePath = row[CompleteAnimeDatas.filePath],
        lastModified = row[CompleteAnimeDatas.lastModified],
        modifiedTime = row[CompleteAnimeDatas.modifiedTime].toInt(),
        rankCnt = row[CompleteAnimeDatas.rankCnt].toInt(),
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r????????????2 = json.decodeFromString(row[CompleteAnimeDatas.r????????????2]),
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r?????? = row[CompleteAnimeDatas.r??????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r????????????All = json.decodeFromString(row[CompleteAnimeDatas.r????????????All]),
        r?????? = row[CompleteAnimeDatas.r??????],
        r?????? = row[CompleteAnimeDatas.r??????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r????????? = row[CompleteAnimeDatas.r?????????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = row[CompleteAnimeDatas.r????????????].toInt(),
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r????????????str = row[CompleteAnimeDatas.r????????????str],
        r????????????str2 = row[CompleteAnimeDatas.r????????????str2],
        r????????????unix = row[CompleteAnimeDatas.r????????????unix].toInt(),
        r?????? = row[CompleteAnimeDatas.r??????],
        r??????2 = json.decodeFromString(row[CompleteAnimeDatas.r??????2]),
        r??????V2 = row[CompleteAnimeDatas.r??????V2],
        r?????? = row[CompleteAnimeDatas.r??????],
        r??????Br = row[CompleteAnimeDatas.r??????_br],
        r?????? = row[CompleteAnimeDatas.r??????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r????????????2 = json.decodeFromString(row[CompleteAnimeDatas.r????????????2]),
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = row[CompleteAnimeDatas.r????????????],
        r???????????? = json.decodeFromString(row[CompleteAnimeDatas.r????????????]),
        r???????????? = json.decodeFromString(row[CompleteAnimeDatas.r????????????])
    )

    override suspend fun allCompleteAnimeData(): List<CompleteAnimeData> = dbQuery {
        CompleteAnimeDatas.selectAll().map(::resultRowToCompleteAnimeData)
    }

    override suspend fun completeAnimeData(aid: String): CompleteAnimeData? = dbQuery {
        CompleteAnimeDatas.select { CompleteAnimeDatas.animeId eq aid }.map(::resultRowToCompleteAnimeData)
            .firstOrNull()
    }

    override suspend fun addNewCompleteAnimeData(
        completeAnimeData: CompleteAnimeData
    ): CompleteAnimeData? = dbQuery {
        CompleteAnimeDatas.insert {
            it[animeId] = completeAnimeData.aID
            it[collectCnt] = completeAnimeData.collectCnt.toString()
            it[commentCnt] = completeAnimeData.commentCnt.toString()
            it[dEFPLAYINDEX] = completeAnimeData.dEFPLAYINDEX
            it[filePath] = completeAnimeData.filePath
            it[lastModified] = completeAnimeData.lastModified
            it[modifiedTime] = completeAnimeData.modifiedTime.toString()
            it[rankCnt] = completeAnimeData.rankCnt.toString()
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????2] = json.encodeToString(completeAnimeData.r????????????2)
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = completeAnimeData.r????????????
            it[r??????] = completeAnimeData.r??????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????All] = json.encodeToString(completeAnimeData.r????????????All)
            it[r??????] = completeAnimeData.r??????
            it[r??????] = completeAnimeData.r??????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = completeAnimeData.r????????????
            it[r?????????] = completeAnimeData.r?????????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = completeAnimeData.r????????????.toString()
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????str] = completeAnimeData.r????????????str
            it[r????????????str2] = completeAnimeData.r????????????str2
            it[r????????????unix] = completeAnimeData.r????????????unix.toString()
            it[r??????] = completeAnimeData.r??????
            it[r??????2] = json.encodeToString(completeAnimeData.r??????2)
            it[r??????V2] = completeAnimeData.r??????V2
            it[r??????] = completeAnimeData.r??????
            it[r??????_br] = completeAnimeData.r??????Br
            it[r??????] = completeAnimeData.r??????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????2] = json.encodeToString(completeAnimeData.r????????????2)
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = completeAnimeData.r????????????
            it[r????????????] = json.encodeToString(completeAnimeData.r????????????)
            it[r????????????] = json.encodeToString(completeAnimeData.r????????????)
        }.resultedValues?.singleOrNull()?.let(::resultRowToCompleteAnimeData)
    }

    override suspend fun updateCompleteAnimeData(
        aid: String,
        completeAnimeData: CompleteAnimeData
    ): Boolean = dbQuery {
        try {
            CompleteAnimeDatas.update {
                it[animeId] = completeAnimeData.aID
                it[collectCnt] = completeAnimeData.collectCnt.toString()
                it[commentCnt] = completeAnimeData.commentCnt.toString()
                it[dEFPLAYINDEX] = completeAnimeData.dEFPLAYINDEX
                it[filePath] = completeAnimeData.filePath
                it[lastModified] = completeAnimeData.lastModified
                it[modifiedTime] = completeAnimeData.modifiedTime.toString()
                it[rankCnt] = completeAnimeData.rankCnt.toString()
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????2] = json.encodeToString(completeAnimeData.r????????????2)
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = completeAnimeData.r????????????
                it[r??????] = completeAnimeData.r??????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????All] = json.encodeToString(completeAnimeData.r????????????All)
                it[r??????] = completeAnimeData.r??????
                it[r??????] = completeAnimeData.r??????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = completeAnimeData.r????????????
                it[r?????????] = completeAnimeData.r?????????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = completeAnimeData.r????????????.toString()
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????str] = completeAnimeData.r????????????str
                it[r????????????str2] = completeAnimeData.r????????????str2
                it[r????????????unix] = completeAnimeData.r????????????unix.toString()
                it[r??????] = completeAnimeData.r??????
                it[r??????2] = json.encodeToString(completeAnimeData.r??????2)
                it[r??????V2] = completeAnimeData.r??????V2
                it[r??????] = completeAnimeData.r??????
                it[r??????_br] = completeAnimeData.r??????Br
                it[r??????] = completeAnimeData.r??????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????2] = json.encodeToString(completeAnimeData.r????????????2)
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = completeAnimeData.r????????????
                it[r????????????] = json.encodeToString(completeAnimeData.r????????????)
                it[r????????????] = json.encodeToString(completeAnimeData.r????????????)
            } > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeCompleteAnimeData(aid: String): Boolean = dbQuery {
        CompleteAnimeDatas.deleteWhere { CompleteAnimeDatas.animeId eq aid } > 0
    }

    private fun resultRowToCloudDiskLink(row: ResultRow) = CloudDiskLink(
        animeId = row[CloudDiskLinks.animeId],
        cloudDiskLinkList = json.decodeFromString(row[CloudDiskLinks.cloudDiskLinkList])
    )

    override suspend fun allCloudDiskLink(): List<CloudDiskLink> = dbQuery {
        CloudDiskLinks.selectAll().map(::resultRowToCloudDiskLink)
    }

    override suspend fun cloudDiskLink(aid: String): CloudDiskLink? = dbQuery {
        CloudDiskLinks.select { CloudDiskLinks.animeId eq aid }.map { resultRowToCloudDiskLink(it) }.firstOrNull()
    }

    override suspend fun addNewCloudDiskLink(cloudDiskLink: CloudDiskLink): CloudDiskLink? = dbQuery {
        CloudDiskLinks.insert {
            it[animeId] = cloudDiskLink.animeId
            it[cloudDiskLinkList] = json.encodeToString(cloudDiskLink.cloudDiskLinkList)
        }.resultedValues?.singleOrNull()?.let(::resultRowToCloudDiskLink)
    }

    override suspend fun updateCloudDiskLink(aid: String, cloudDiskLinkList: MutableList<AnimeInfosEntity.AniInfoEntity.R????????????2Entity>): Boolean = dbQuery {
        try {
            CloudDiskLinks.update({ CloudDiskLinks.animeId eq aid }) {
                it[CloudDiskLinks.cloudDiskLinkList] = json.encodeToString(cloudDiskLinkList)
            } > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeCloudDiskLink(aid: String): Boolean = dbQuery {
        CloudDiskLinks.deleteWhere { CloudDiskLinks.animeId eq aid } > 0
    }

    private fun resultRowToPlayerNumber(row: ResultRow) = PlayerNumber(
        aid = row[PlayerNumbers.aid],
        playList = json.decodeFromString(row[PlayerNumbers.playList])
    )

    override suspend fun allPlayList(): List<PlayerNumber> = dbQuery {
        PlayerNumbers.selectAll().map { resultRowToPlayerNumber(it) }
    }

    override suspend fun playLists(aid: String): PlayerNumber? = dbQuery {
        PlayerNumbers.select { PlayerNumbers.aid eq aid }.map { resultRowToPlayerNumber(it) }.firstOrNull()
    }

    override suspend fun addNewPlayList(
        aid: String,
        playList: MutableList<MutableList<AnimeInfosEntity.AniInfoEntity.R????????????AllEntity>>,
    ): PlayerNumber? = dbQuery {
        PlayerNumbers.insert {
            it[PlayerNumbers.aid] = aid
            it[PlayerNumbers.playList] = json.encodeToString(playList)
        }.resultedValues?.singleOrNull()?.let(::resultRowToPlayerNumber)
    }

    override suspend fun updatePlayList(
        aid: String,
        newPlayList: MutableList<MutableList<AnimeInfosEntity.AniInfoEntity.R????????????AllEntity>>,
    ): Boolean = dbQuery {
        try {
            PlayerNumbers.update({ PlayerNumbers.aid eq aid }) {
                it[playList] = json.encodeToString(newPlayList)
            } > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removePlayList(aid: String): Boolean = dbQuery {
        PlayerNumbers.deleteWhere { PlayerNumbers.aid eq aid } > 0
    }

}

val dao: DAOFacade = DAOFacadeImpl().apply {
    runBlocking {
        if (allUsers().isEmpty()) {
            addNewUser(User(
                id = 0,
                name = "default",
                pass = "123456789",
                email = "",
                portraitUrl = defaultPortraitUrl,
                favoriteData = User.FavoriteDataDTO(),
                historyData = User.HistoryDataDTO(),
                chaseAnimeData = User.ChaseAnimeDataDTO(),
                created_at = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            )
        }

        if (allSimpleAnimeData().isEmpty()) {
            addNewSimpleAnimeData(SimpleAnimeData())
        }

        if (allCompleteAnimeData().isEmpty()) {
            addNewCompleteAnimeData(CompleteAnimeData())
        }

        if (allPlayList().isEmpty()) {
            addNewPlayList("1", mutableListOf(mutableListOf()))
        }

        if (allCloudDiskLink().isEmpty()) {
            addNewCloudDiskLink(CloudDiskLink("1", mutableListOf()))
        }

    }
}
