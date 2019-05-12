package org.bytekeeper.bacordi.entity

import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import javax.persistence.*

enum class Race(val short: String) {
    ZERG("Z"),
    TERRAN("T"),
    PROTOSS("P"),
    RANDOM("R");
}

@Entity
class BotElo(
    @Id val id: Long,
    @ManyToOne(fetch = FetchType.LAZY) val bot: Bot,
    val time: Instant,
    val rating: Int,
    @ManyToOne(fetch = FetchType.LAZY) val game: GameResult
)

@Entity
class Bot(
    @Id val id: Long,
    var enabled: Boolean = true,
    var disabledReason: String? = null,
    var name: String,
    @Enumerated(EnumType.STRING) var race: Race,
    var botType: String,
    var lastUpdated: Instant? = null,
    var publishRead: Boolean = false,
    var authorKeyId: String? = null,
    var played: Int = 0,
    var rating: Int = 2000,
    var crashed: Int = 0,
    var crashesSinceUpdate: Int = 0,
    var won: Int = 0,
    var lost: Int = 0,
    var mapPools: String = ""
) {
    fun mapPools() = mapPools.split(",").filter { it.isNotBlank() }
}

@Entity
class BotHistory(
    @Id val id: Long,
    @ManyToOne(fetch = FetchType.LAZY) val bot: Bot,
    val time: Instant,
    val mapPools: String
)

@Transactional(readOnly = true)
interface BotRepository : CrudRepository<Bot, Long> {
    fun findByName(name: String): Bot?
}


@Transactional(readOnly = true)
interface BotEloRepository : CrudRepository<BotElo, Long>


@Transactional(readOnly = true)
interface BotHistoryRepository : CrudRepository<BotHistory, Long>