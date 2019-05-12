package org.bytekeeper.bacordi.entity

import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class BasilSourceBot(
    @Id var id: Long? = null,
    var name: String,
    var lastUpdated: Instant?,
    var hash: String
)

@Transactional(readOnly = true)
interface BasilSourceBotRepository : CrudRepository<BasilSourceBot, Long> {
    fun findByName(name: String): BasilSourceBot?
}
