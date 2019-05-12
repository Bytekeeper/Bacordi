package org.bytekeeper.bacordi

import net.dv8tion.jda.core.entities.MessageChannel
import org.bytekeeper.bacordi.entity.BotRepository
import org.bytekeeper.bacordi.entity.GameResultRepository
import org.bytekeeper.bacordi.entity.Race
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import kotlin.streams.asSequence

@Service
@Async
class Queries(
    private val gameResultRepository: GameResultRepository,
    private val botRepository: BotRepository
) {
    @RateLimit
    fun respondGamesPlayed(channel: MessageChannel) {
        channel.sendMessage("BASIL played ${gameResultRepository.count()} games.").queue()
        val lastGame = gameResultRepository.findFirstByOrderByTimeDesc() ?: return
        channel.sendMessage("Last finished game was ${lastGame.botA.name} vs ${lastGame.botB.name}.").queue()
    }

    @RateLimit
    fun respondStatus(channel: MessageChannel) {
        val time = gameResultRepository.findFirstByOrderByTimeDesc()?.time
        if (time == null) {
            channel.sendMessage("Uh oh, no games are played as of yet!").queue()
        } else {
            channel.sendMessage("Last game finished: $time").queue()
            if (time.isBefore(Instant.now().minus(Duration.ofMinutes(60)))) {
                channel.sendMessage("BASIL appears to be down.")
            } else {
                channel.sendMessage("BASIL appears to be running.")
            }.queue()
        }
    }

    @RateLimit
    @Transactional
    fun respondTitle(channel: MessageChannel, botName: String) {
        if (botName.isEmpty()) return
        val bot = botRepository.findByName(botName) ?: run {
            channel.sendMessage("No such bot: $botName").queue()
            return
        }
        if (!bot.enabled) {
            channel.sendMessage("Sorry, $botName is on hiatus.").queue()
            return
        }
        channel.sendMessage("'$botName' is known as").queue()

        val titleAccumulator = gameResultRepository.findByBotAOrBotB(bot)
            .use {
                it.asSequence().fold(TitleAccumulator()) { acc, gr ->
                    if (gr.winner == bot) {
                        acc.winLose.won++
                        when (gr.loser?.race) {
                            Race.PROTOSS -> acc.vsProtoss.won++
                            Race.ZERG -> acc.vsZerg.won++
                            Race.TERRAN -> acc.vsTerran.won++
                            else -> Unit
                        }
                        acc.perMapWinLose.computeIfAbsent(gr.map) { WinLose(0, 0) }.won++
                    }
                    if (gr.loser == bot) {
                        acc.winLose.lost++
                        when (gr.winner?.race) {
                            Race.PROTOSS -> acc.vsProtoss.lost++
                            Race.ZERG -> acc.vsZerg.lost++
                            Race.TERRAN -> acc.vsTerran.lost++
                            else -> Unit
                        }
                        acc.perMapWinLose.computeIfAbsent(gr.map) { WinLose(0, 0) }.lost++
                    }
                    if (gr.botACrashed && gr.botA == bot || gr.botBCrashed && gr.botB == bot) acc.crashed++
                    acc.games++
                    acc
                }
            }
        channel.sendMessage(TitleBuilder(titleAccumulator).toString(bot.name)).queue()
    }
}

private class TitleAccumulator(
    var winLose: WinLose = WinLose(0, 0),
    var vsProtoss: WinLose = WinLose(0, 0),
    var vsTerran: WinLose = WinLose(0, 0),
    var vsZerg: WinLose = WinLose(0, 0),
    var perMapWinLose: MutableMap<String, WinLose> = mutableMapOf(),
    var crashed: Int = 0,
    var games: Int = 0
)

private class WinLose(var won: Int, var lost: Int) {
    fun <T> indexInto(list: List<T>, bias: Double = 0.0) =
        if (won + lost > 0) list[(list.size * (won.toDouble() / (won + lost) - bias) / (1.0 - bias)).toInt()] else null

    fun betterThan(rating: Double) = (won + lost) * rating < won
    val rate get() = if (won + lost > 0) won.toDouble() / (won + lost) else null
}

private class TitleBuilder(private val acc: TitleAccumulator) {
    private val performerTitle =
        listOf(
            "flow heater",
            "office bore",
            "mediocre",
            "unexceptional",
            "ordinary",
            "destroyer",
            "glorious",
            "punisher"
        )

    private val vsZergTitle =
        listOf(
            "bug-smasher", "antlion", "exterminator"
        )

    private val vsProtossTitle =
        listOf(
            "psi-breaker", "technomage", "photon-master"
        )
    private val vsTerranTitle =
        listOf(
            "rust remover", "piston jammer", "wrecker"
        )

    private val animalNames = listOf(
        "albatross", "alligator", "ape", "bat", "beaver", "boar", "cat", "cobra", "deer", "eagle", "eel",
        "ferret", "fox", "goose", "gorilla", "hawk", "herring", "ibis", "jaguar", "kangaroo", "kudu",
        "lemur", "lion", "manatee", "mongoose", "mouse", "nightingale", "octopus", "opossum", "parrot",
        "penguin", "porpoise", "rabbit", "raccoon", "rhinoceros", "salamander", "seal",
        "shark", "snake", "tapir", "tiger", "turtle", "walrus", "wasp", "wolf", "wombat",
        "yak", "zebra"
    )

    private val locations = listOf(
        "of the north", "of the south", "of the west", "of the east", "of the void",
        "of the bathroom"
    )

    fun toString(name: String): String {
        val rnd = Random(name.hashCode())
        val nameA = animalNames.random(rnd)
        val locationA = locations.random(rnd)

        val performer = acc.winLose.indexInto(performerTitle) ?: "mysterious"
        val sb = StringBuilder()
        sb.append(name).append(" the ").append(performer)
        if (acc.vsZerg.betterThan(0.7)) sb.append(", ").append(acc.vsZerg.indexInto(vsZergTitle, 0.7))
        if (acc.vsProtoss.betterThan(0.7)) sb.append(", ").append(acc.vsProtoss.indexInto(vsProtossTitle, 0.7))
        if (acc.vsTerran.betterThan(0.7)) sb.append(", ").append(acc.vsTerran.indexInto(vsTerranTitle, 0.7))
        if (acc.games * 0.1 < acc.crashed) sb.append(", mentally unstable")
        val mapRatings = acc.perMapWinLose.mapNotNull { it.value.rate }
        val mapMinRate = mapRatings.min()
        val mapMaxRate = mapRatings.max()
        if ((mapMinRate ?: 1.0) / (mapMaxRate ?: 1.0) < 0.66) sb.append(", schizophrenic $nameA")
        else if ((mapMinRate ?: 1.0) / (mapMaxRate ?: 1.0) > 0.88) sb.append(", steady $nameA $locationA")
        return sb.toString()
    }
}