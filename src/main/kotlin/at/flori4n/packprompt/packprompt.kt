package at.flori4n.packprompt

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Plugin(
    id = "packprompt",
    name = "Velocity Pack Prompt",
    version = "1.0.0",
    description = "forces pack prompts for velocity",
    authors = ["flori4_"]
)
class MyPlugin @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path
) {

    private lateinit var settings: PackSettings

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("My Plugin has been initialized!")
        settings = loadSettings()

        logger.info(
            "Velocity is running with {} players.",
            server.playerCount
        )
    }

    @Subscribe
    fun onPostConnect(event: ServerPostConnectEvent) {
        // Only on initial proxy join, not on every server switch.
        if (event.previousServer != null) return

        val player = event.player
        // Delay so the lobby join fully completes first, otherwise
        // the backend server clears the prompt right away.
        server.scheduler.buildTask(this, Runnable {
            if (!player.isActive) return@Runnable
            val builder = server.createResourcePackBuilder(settings.url)
                .setPrompt(Component.text(settings.prompt))
                .setShouldForce(settings.force)
            parseHash(settings.hash)?.let { builder.setHash(it) }
            player.sendResourcePackOffer(builder.build())
        }).delay(settings.delaySeconds, TimeUnit.SECONDS).schedule()
    }

    private fun loadSettings(): PackSettings {
        Files.createDirectories(dataDirectory)
        val configFile = dataDirectory.resolve("config.yml")
        if (Files.notExists(configFile)) {
            javaClass.getResourceAsStream("/config.yml")?.use { input ->
                Files.copy(input, configFile)
            }
        }
        val node = YamlConfigurationLoader.builder()
            .path(configFile)
            .build()
            .load()
        return PackSettings(
            url = node.node("pack", "url").getString("https://example.com/pack.zip")!!,
            prompt = node.node("pack", "prompt").getString("Please accept our resource pack to play!")!!,
            force = node.node("pack", "force").getBoolean(true),
            hash = node.node("pack", "hash").getString("")!!,
            delaySeconds = node.node("delay-seconds").getLong(1).coerceAtLeast(0)
        )
    }

    data class PackSettings(
        val url: String,
        val prompt: String,
        val force: Boolean,
        val hash: String,
        val delaySeconds: Long
    )

    /** Parses a SHA-1 hex string (40 chars) or returns null if blank/invalid. */
    private fun parseHash(hex: String): ByteArray? {
        val clean = hex.trim()
        if (clean.isEmpty()) return null
        if (!clean.matches(Regex("[0-9a-fA-F]{40}"))) {
            logger.warn("Invalid pack hash '{}', expected 40 hex chars (SHA-1). Sending pack without hash.", hex)
            return null
        }
        return ByteArray(20) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}
