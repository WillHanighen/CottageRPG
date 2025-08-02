package dev.cottage.cottageRPG.player

import dev.cottage.cottageRPG.CottageRPG
import dev.cottage.cottageRPG.database.DatabaseManager
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages all RPG player data and operations
 */
class PlayerManager(private val plugin: CottageRPG) {
    
    private val players = ConcurrentHashMap<UUID, RPGPlayer>()
    val databaseManager = DatabaseManager(plugin)
    
    init {
        // Start periodic save task
        startPeriodicSave()
        // Start mana regeneration task
        startManaRegeneration()
    }
    
    /**
     * Get or create an RPG player
     */
    fun getPlayer(uuid: UUID): RPGPlayer {
        return players.computeIfAbsent(uuid) { 
            loadPlayerFromDatabase(uuid) ?: createNewPlayer(uuid)
        }
    }
    
    /**
     * Get an RPG player by Bukkit player
     */
    fun getPlayer(player: Player): RPGPlayer {
        return getPlayer(player.uniqueId)
    }
    
    /**
     * Create a new RPG player with default values
     */
    private fun createNewPlayer(uuid: UUID): RPGPlayer {
        val config = plugin.configManager
        return RPGPlayer(
            uuid = uuid,
            level = config.getInt("player.starting_level", 1),
            health = config.getDouble("player.starting_health", 20.0),
            maxHealth = config.getDouble("player.starting_health", 20.0),
            mana = config.getDouble("player.starting_mana", 100.0),
            maxMana = config.getDouble("player.starting_mana", 100.0),
            money = config.getDouble("economy.starting_money", 100.0)
        )
    }
    
    /**
     * Load player data from database
     */
    private fun loadPlayerFromDatabase(uuid: UUID): RPGPlayer? {
        return try {
            databaseManager.loadPlayer(uuid)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load player data for $uuid: ${e.message}")
            null
        }
    }
    
    /**
     * Save player data to database
     */
    fun savePlayer(rpgPlayer: RPGPlayer) {
        try {
            databaseManager.savePlayer(rpgPlayer)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to save player data for ${rpgPlayer.uuid}: ${e.message}")
        }
    }
    
    /**
     * Handle player join
     */
    fun onPlayerJoin(player: Player) {
        val rpgPlayer = getPlayer(player.uniqueId)
        rpgPlayer.isOnline = true
        rpgPlayer.updateLastSeen()
        
        // Apply health and mana to Bukkit player
        player.maxHealth = rpgPlayer.maxHealth
        player.health = rpgPlayer.health
        
        if (plugin.configManager.getBoolean("general.debug")) {
            plugin.logger.info("Player ${player.name} joined with RPG data loaded")
        }
    }
    
    /**
     * Handle player quit
     */
    fun onPlayerQuit(player: Player) {
        val rpgPlayer = getPlayer(player.uniqueId)
        rpgPlayer.isOnline = false
        rpgPlayer.updateLastSeen()
        
        // Update health from Bukkit player
        rpgPlayer.health = player.health
        
        // Save player data
        savePlayer(rpgPlayer)
        
        if (plugin.configManager.getBoolean("general.debug")) {
            plugin.logger.info("Player ${player.name} quit and data saved")
        }
    }
    
    /**
     * Get all online RPG players
     */
    fun getOnlinePlayers(): Collection<RPGPlayer> {
        return players.values.filter { it.isOnline }
    }
    
    /**
     * Get all loaded RPG players
     */
    fun getAllPlayers(): Collection<RPGPlayer> {
        return players.values
    }
    
    /**
     * Remove player from cache (useful for memory management)
     */
    fun unloadPlayer(uuid: UUID) {
        players[uuid]?.let { rpgPlayer ->
            if (!rpgPlayer.isOnline) {
                savePlayer(rpgPlayer)
                players.remove(uuid)
            }
        }
    }
    
    /**
     * Save all player data
     */
    fun saveAllPlayers() {
        players.values.forEach { rpgPlayer ->
            savePlayer(rpgPlayer)
        }
    }
    
    /**
     * Start periodic save task
     */
    private fun startPeriodicSave() {
        val saveInterval = plugin.configManager.getLong("player.save_interval", 6000L)
        
        object : BukkitRunnable() {
            override fun run() {
                saveAllPlayers()
                
                if (plugin.configManager.getBoolean("general.debug")) {
                    plugin.logger.info("Periodic save completed for ${players.size} players")
                }
            }
        }.runTaskTimerAsynchronously(plugin, saveInterval, saveInterval)
    }
    
    /**
     * Start mana regeneration task
     */
    private fun startManaRegeneration() {
        object : BukkitRunnable() {
            override fun run() {
                getOnlinePlayers().forEach { rpgPlayer ->
                    val regenAmount = rpgPlayer.maxMana * 0.01 // 1% per second
                    rpgPlayer.regenerateMana(regenAmount)
                }
            }
        }.runTaskTimer(plugin, 20L, 20L) // Run every second
    }
    
    /**
     * Shutdown and save all data
     */
    fun shutdown() {
        saveAllPlayers()
        databaseManager.close()
    }
    
    /**
     * Reload player manager
     */
    fun reload() {
        // Save current data
        saveAllPlayers()
        
        // Clear cache for offline players
        players.entries.removeIf { !it.value.isOnline }
        
        plugin.logger.info("Player manager reloaded")
    }
}
