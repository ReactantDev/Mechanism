package dev.reactant.mechanism

import dev.reactant.reactant.core.ReactantPlugin
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bukkit.plugin.java.JavaPlugin

@ReactantPlugin(["dev.reactant.mechanism"])
class Mechanism : JavaPlugin() {

    companion object {
        @JvmStatic
        val logger: Logger = LogManager.getLogger("Mechanism")
    }

}
