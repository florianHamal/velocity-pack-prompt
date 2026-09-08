package at.flori4n.packprompt

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import org.slf4j.Logger
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
    private val logger: Logger
) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("My Plugin has been initialized!")

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
            val pack = server.createResourcePackBuilder(PACK_URL)
                .setPrompt(Component.text(PACK_PROMPT))
                .setShouldForce(PACK_FORCE)
                .build()
            player.sendResourcePackOffer(pack)
        }).delay(1, TimeUnit.SECONDS).schedule()
    }

    companion object {
        private const val PACK_URL = "https://example.com/pack.zip"
        private const val PACK_PROMPT = "Please accept our resource pack to play!"
        private const val PACK_FORCE = true
    }
}
