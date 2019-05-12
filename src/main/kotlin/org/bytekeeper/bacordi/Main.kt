package org.bytekeeper.bacordi

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.apache.logging.log4j.LogManager
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.startsWith
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Service
import javax.annotation.PreDestroy

@ConfigurationProperties("bacordi")
@Configuration
class Config {
    lateinit var token: String
}


@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
class Bacordi {
    @Bean
    fun jda(config: Config) = JDABuilder(config.token).build()
}

@Service
class Bot(
    private val jda: JDA,
    private val queries: Queries
) {
    private val log = LogManager.getLogger()

    @PreDestroy
    fun logoff() {
        jda.shutdownNow()
    }

    fun run() {
        jda.awaitReady()
        jda.addEventListener(object : ListenerAdapter() {
            override fun onMessageReceived(event: MessageReceivedEvent) {
                if (event.author == jda.selfUser) return
                if (event.isFromType(ChannelType.PRIVATE)) {
                    event.channel.sendMessage("No requests per PM.").queue()
                    return
                }

                if (!event.isFromType(ChannelType.TEXT)) return
                if (event.channel.name != "basil") return
                log.debug("Got {} from {}", event.message.contentDisplay, event.author.name)
                when (val cmd = event.message.contentDisplay) {
                    "!games" -> queries.respondGamesPlayed(event.channel)
                    "!status" -> queries.respondStatus(event.channel)
                    else -> when {
                        cmd.startsWith("!title") -> queries.respondTitle(event.channel, cmd.substringAfter("!title").trim())
                    }
                }
            }
        })
    }
}

fun main() {
    val context = SpringApplication.run(Bacordi::class.java)
    context.getBean(Bot::class.java).run()
}