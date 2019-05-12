package org.bytekeeper.bacordi.entity

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*
import java.util.stream.Stream
import javax.persistence.*

@Entity
data class GameResult(
    @Id val id: UUID,
    val time: Instant,
    val gameRealtime: Double,
    val realtimeTimeout: Boolean = false,
    val frameTimeout: Boolean? = false,
    val mapPool: String,
    val map: String,
    @ManyToOne(fetch = FetchType.LAZY) val botA: Bot,
    @Enumerated(EnumType.STRING) val raceA: Race,
    @ManyToOne(fetch = FetchType.LAZY) val botB: Bot,
    @Enumerated(EnumType.STRING) val raceB: Race,
    @ManyToOne(fetch = FetchType.LAZY) val winner: Bot? = null,
    @ManyToOne(fetch = FetchType.LAZY) val loser: Bot? = null,
    val botACrashed: Boolean = false,
    val botBCrashed: Boolean = false,
    val gameHash: String,
    val frameCount: Int? = null
)

@Transactional(readOnly = true)
interface GameResultRepository : CrudRepository<GameResult, Long> {
    fun findFirstByOrderByTimeDesc(): GameResult?

    @Query("select g from #{#entityName} g where botA = :bot or botB = :bot")
    fun findByBotAOrBotB(@Param("bot") bot: Bot) : Stream<GameResult>
}