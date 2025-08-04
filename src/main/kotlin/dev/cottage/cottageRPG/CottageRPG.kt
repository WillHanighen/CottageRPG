package dev.cottage.cottageRPG

import dev.cottage.cottageRPG.commands.RPGCommand
import dev.cottage.cottageRPG.config.ConfigManager
import dev.cottage.cottageRPG.database.DatabaseManager
import dev.cottage.cottageRPG.listeners.PlayerEventListener
import dev.cottage.cottageRPG.player.PlayerManager
import dev.cottage.cottageRPG.scoreboard.ScoreboardManager
import dev.cottage.cottageRPG.ui.BossBarManager
import dev.cottage.cottageRPG.crafting.WandCraftingManager
import dev.cottage.cottageRPG.spells.PlayerSpellManager
import dev.cottage.cottageRPG.spells.SpellCooldownManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.plugin.java.JavaPlugin

class CottageRPG : JavaPlugin() {

    lateinit var configManager: ConfigManager
    lateinit var databaseManager: DatabaseManager
    lateinit var playerManager: PlayerManager
    lateinit var scoreboardManager: ScoreboardManager
    lateinit var bossBarManager: BossBarManager
    lateinit var wandCraftingManager: WandCraftingManager
    lateinit var playerSpellManager: PlayerSpellManager
    lateinit var spellCooldownManager: SpellCooldownManager

    override fun onEnable() {
        // Initialize managers
        configManager = ConfigManager(this)
        databaseManager = DatabaseManager(this)
        playerManager = PlayerManager(this)
        scoreboardManager = ScoreboardManager(this)
        bossBarManager = BossBarManager(this)
        wandCraftingManager = WandCraftingManager(this)
        playerSpellManager = PlayerSpellManager(this)
        spellCooldownManager = SpellCooldownManager()

        // Register commands
        val rpgCommand = RPGCommand(this)
        getCommand("rpg")?.setExecutor(rpgCommand)
        getCommand("rpg")?.tabCompleter = rpgCommand
        getCommand("stats")?.setExecutor(rpgCommand)
        getCommand("stats")?.tabCompleter = rpgCommand
        getCommand("skills")?.setExecutor(rpgCommand)
        getCommand("skills")?.tabCompleter = rpgCommand
        getCommand("class")?.setExecutor(rpgCommand)
        getCommand("class")?.tabCompleter = rpgCommand
        getCommand("scoreboard")?.setExecutor(rpgCommand)
        getCommand("scoreboard")?.tabCompleter = rpgCommand


        // Register event listeners using PaperMC's server instance
        server.pluginManager.registerEvents(PlayerEventListener(this), this)

        // Register crafting recipes
        wandCraftingManager.registerWandRecipe()

        // Plugin startup logic with Adventure Component
        val enableMessage = Component.text("CottageRPG v")
            .color(NamedTextColor.GREEN)
            .append(Component.text(pluginMeta.version, NamedTextColor.YELLOW))
            .append(Component.text(" has been enabled!", NamedTextColor.GREEN))
        
        // Use PaperMC's enhanced logging with Component support
        componentLogger.info(enableMessage)
        
        // Also log to console for compatibility
        logger.info("CottageRPG v${pluginMeta.version} has been enabled!")
    }

    override fun onDisable() {
        // Save spell selections before shutdown
        if (::playerSpellManager.isInitialized) {
            playerSpellManager.forceSave()
        }
        
        // Unregister crafting recipes
        if (::wandCraftingManager.isInitialized) {
            wandCraftingManager.unregisterRecipes()
        }
        
        // Shutdown scoreboard manager
        if (::scoreboardManager.isInitialized) {
            scoreboardManager.shutdown()
        }
        
        // Shutdown boss bar manager
        if (::bossBarManager.isInitialized) {
            bossBarManager.removeAllBossBars()
        }
        
        // Close database connection if needed
        if (::databaseManager.isInitialized) {
            databaseManager.close()
        }
        
        // Plugin shutdown logic with Adventure Component
        val disableMessage = Component.text("CottageRPG v")
            .color(NamedTextColor.RED)
            .append(Component.text(pluginMeta.version, NamedTextColor.YELLOW))
            .append(Component.text(" has been disabled!", NamedTextColor.RED))
        
        // Use PaperMC's enhanced logging with Component support
        componentLogger.info(disableMessage)
        
        // Also log to console for compatibility
        logger.info("CottageRPG v${pluginMeta.version} has been disabled!")
    }

    /**
     * Get the server instance using PaperMC's preferred method
     */
    fun getServerInstance() = server

    /**
     * Broadcast a message to all players using Adventure Components
     */
    fun broadcastComponent(component: Component) {
        server.sendMessage(component)
    }

    /**
     * Broadcast a message to all players with permission using Adventure Components
     */
    fun broadcastComponent(component: Component, permission: String) {
        server.broadcast(component, permission)
    }

    /**
     * Create a formatted plugin message component
     */
    fun createPluginMessage(message: String, color: NamedTextColor = NamedTextColor.AQUA): Component {
        return Component.text("[CottageRPG] ", NamedTextColor.DARK_AQUA)
            .append(Component.text(message, color))
    }

    /**
     * Create an error message component
     */
    fun createErrorMessage(message: String): Component {
        return Component.text("[CottageRPG] ", NamedTextColor.DARK_RED)
            .append(Component.text("Error: ", NamedTextColor.RED))
            .append(Component.text(message, NamedTextColor.YELLOW))
    }

    /**
     * Create a success message component
     */
    fun createSuccessMessage(message: String): Component {
        return Component.text("[CottageRPG] ", NamedTextColor.DARK_GREEN)
            .append(Component.text(message, NamedTextColor.GREEN))
    }
}
