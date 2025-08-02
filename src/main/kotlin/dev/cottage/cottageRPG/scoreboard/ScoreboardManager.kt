package dev.cottage.cottageRPG.scoreboard

import dev.cottage.cottageRPG.CottageRPG
import dev.cottage.cottageRPG.player.RPGPlayer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the RPG scoreboard display for players
 * Shows class, money, abilities, cooldowns, mana, and other RPG information
 */
class ScoreboardManager(private val plugin: CottageRPG) {
    
    private val playerScoreboards = ConcurrentHashMap<UUID, Scoreboard>()
    private var updateTask: BukkitRunnable? = null
    
    init {
        startUpdateTask()
    }
    
    /**
     * Create and show scoreboard for a player
     */
    fun createScoreboard(player: Player) {
        if (!plugin.configManager.getBoolean("scoreboard.enabled", true)) {
            return
        }
        
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val scoreboard = Bukkit.getScoreboardManager()?.newScoreboard ?: return
        
        val objective = scoreboard.registerNewObjective(
            "rpg_info",
            "dummy",
            plugin.configManager.getString("scoreboard.title", "§6§lCottage RPG")
        )
        objective.displaySlot = DisplaySlot.SIDEBAR
        
        updateScoreboardContent(player, rpgPlayer, objective)
        
        player.scoreboard = scoreboard
        playerScoreboards[player.uniqueId] = scoreboard
    }
    
    /**
     * Update scoreboard content for a player
     */
    private fun updateScoreboardContent(player: Player, rpgPlayer: RPGPlayer, objective: Objective) {
        // Sync RPGPlayer health with Bukkit player health before updating scoreboard
        rpgPlayer.health = Math.floor(player.getHealth() * 2) / 2.0;
        rpgPlayer.maxHealth = player.maxHealth
        
        // Clear existing scores
        objective.scoreboard?.entries?.forEach { entry ->
            objective.scoreboard?.resetScores(entry)
        }
        
        var line = 15 // Start from top
        
        // Empty line for spacing
        objective.getScore("§r").score = line--
        
        // Player Level and Experience
        if (plugin.configManager.getBoolean("scoreboard.display.level", true)) {
            val expToNext = rpgPlayer.getExperienceForNextLevel()
            val expProgress = if (expToNext > 0) {
                val current = rpgPlayer.experience % expToNext
                val percentage = (current.toDouble() / expToNext * 100).toInt()
                "§7($percentage%)"
            } else "§7(MAX)"
            
            objective.getScore("§6§lLevel: §f${rpgPlayer.level} $expProgress").score = line--
        }
        
        // Class Information
        if (plugin.configManager.getBoolean("scoreboard.display.class", true)) {
            val className = rpgPlayer.rpgClass?.displayName ?: "§7None"
            val classColor = rpgPlayer.rpgClass?.let { "§a" } ?: "§7"
            objective.getScore("§e§lClass: $classColor$className").score = line--
        }
        
        // Empty line
        objective.getScore("§1").score = line--
        
        // Health and Mana
        if (plugin.configManager.getBoolean("scoreboard.display.health", true)) {
            val healthBar = createBar(rpgPlayer.health, rpgPlayer.maxHealth, "§c❤", "§8❤", 10)
            objective.getScore("§c§lHealth: $healthBar").score = line--
        }
        
        if (plugin.configManager.getBoolean("scoreboard.display.mana", true)) {
            val manaBar = createBar(rpgPlayer.mana, rpgPlayer.maxMana, "§9✦", "§8✦", 10)
            objective.getScore("§9§lMana: $manaBar").score = line--
        }
        
        // Empty line
        objective.getScore("§2").score = line--
        
        // Money
        if (plugin.configManager.getBoolean("scoreboard.display.money", true)) {
            val money = String.format("%.2f", rpgPlayer.money)
            objective.getScore("§2§lMoney: §f$${money}").score = line--
        }
        
        // Empty line
        objective.getScore("§3").score = line--
        
        // Top Skills
        if (plugin.configManager.getBoolean("scoreboard.display.top_skills", true)) {
            val skillCount = plugin.configManager.getInt("scoreboard.display.top_skills_count", 3)
            val topSkills = rpgPlayer.skills.entries
                .sortedByDescending { it.value }
                .take(skillCount)
            
            if (topSkills.isNotEmpty()) {
                objective.getScore("§d§lTop Skills:").score = line--
                topSkills.forEach { (skill, level) ->
                    val skillName = skill.replaceFirstChar { it.uppercase() }
                    objective.getScore("§7• $skillName: §f$level").score = line--
                }
            }
        }
        
        // Active Effects/Cooldowns
        if (plugin.configManager.getBoolean("scoreboard.display.cooldowns", true)) {
            val maxCooldowns = plugin.configManager.getInt("scoreboard.display.max_cooldowns", 2)
            val activeCooldowns = getActiveCooldowns(rpgPlayer)
            if (activeCooldowns.isNotEmpty()) {
                objective.getScore("§c§lCooldowns:").score = line--
                activeCooldowns.take(maxCooldowns).forEach { cooldown ->
                    objective.getScore("§7• $cooldown").score = line--
                }
            }
        }
        
        // Server info at bottom
        objective.getScore("§5").score = line--
        val serverName = plugin.configManager.getString("scoreboard.server_name", "CottageRPG")
        objective.getScore("§7$serverName").score = line--
    }
    
    /**
     * Create a visual bar representation
     */
    private fun createBar(current: Double, max: Double, filledChar: String, emptyChar: String, length: Int): String {
        val percentage = if (max > 0) current / max else 0.0
        val filled = (percentage * length).toInt()
        val empty = length - filled
        
        return filledChar.repeat(filled) + emptyChar.repeat(empty) + 
               " §f${current.toInt()}§7/§f${max.toInt()}"
    }
    
    /**
     * Get active cooldowns for a player (placeholder for future ability system)
     */
    private fun getActiveCooldowns(rpgPlayer: RPGPlayer): List<String> {
        // This is a placeholder - in the future, this would check actual ability cooldowns
        val cooldowns = mutableListOf<String>()
        
        // Example cooldowns (these would be real in a full implementation)
        if (rpgPlayer.rpgClass != null) {
            when (rpgPlayer.rpgClass!!.id.lowercase()) {
                "warrior" -> {
                    // Example: if warrior has used charge ability recently
                    // cooldowns.add("Charge: 5s")
                }
                "mage" -> {
                    // Example: if mage has used fireball recently
                    // cooldowns.add("Fireball: 3s")
                }
                // Add more class-specific cooldowns
            }
        }
        
        return cooldowns
    }
    
    /**
     * Update scoreboard for a specific player
     */
    fun updatePlayerScoreboard(player: Player) {
        val scoreboard = playerScoreboards[player.uniqueId] ?: return
        val objective = scoreboard.getObjective("rpg_info") ?: return
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        
        updateScoreboardContent(player, rpgPlayer, objective)
    }
    
    /**
     * Remove scoreboard for a player
     */
    fun removeScoreboard(player: Player) {
        playerScoreboards.remove(player.uniqueId)
        // Reset to default scoreboard
        val mainScoreboard = Bukkit.getScoreboardManager()?.mainScoreboard
        if (mainScoreboard != null) {
            player.scoreboard = mainScoreboard
        }
    }
    
    /**
     * Start the update task that refreshes scoreboards
     */
    private fun startUpdateTask() {
        val updateInterval = plugin.configManager.getInt("scoreboard.update_interval", 20) // Default 1 second
        
        updateTask = object : BukkitRunnable() {
            override fun run() {
                if (!plugin.configManager.getBoolean("scoreboard.enabled", true)) {
                    return
                }
                
                Bukkit.getOnlinePlayers().forEach { player ->
                    if (playerScoreboards.containsKey(player.uniqueId)) {
                        updatePlayerScoreboard(player)
                    }
                }
            }
        }
        
        updateTask?.runTaskTimer(plugin, 0L, updateInterval.toLong())
    }
    
    /**
     * Stop the update task
     */
    fun shutdown() {
        updateTask?.cancel()
        updateTask = null
        
        // Remove all scoreboards
        Bukkit.getOnlinePlayers().forEach { player ->
            removeScoreboard(player)
        }
        playerScoreboards.clear()
    }
    
    /**
     * Toggle scoreboard for a player
     */
    fun toggleScoreboard(player: Player): Boolean {
        return if (playerScoreboards.containsKey(player.uniqueId)) {
            removeScoreboard(player)
            false
        } else {
            createScoreboard(player)
            true
        }
    }
    
    /**
     * Check if player has scoreboard enabled
     */
    fun hasScoreboard(player: Player): Boolean {
        return playerScoreboards.containsKey(player.uniqueId)
    }
    
    /**
     * Enable scoreboard for a player
     */
    fun enableScoreboard(player: Player) {
        createScoreboard(player)
    }
    
    /**
     * Disable scoreboard for a player
     */
    fun disableScoreboard(player: Player) {
        removeScoreboard(player)
    }
    
    /**
     * Check if scoreboard is enabled for a player
     */
    fun isScoreboardEnabled(player: Player): Boolean {
        return hasScoreboard(player)
    }
    
    /**
     * Force immediate update for a specific player (called when important data changes)
     */
    fun forceUpdatePlayer(player: Player) {
        if (playerScoreboards.containsKey(player.uniqueId)) {
            updatePlayerScoreboard(player)
        }
    }
    
    /**
     * Update scoreboard when player gains experience
     */
    fun onPlayerExperienceGain(player: Player) {
        forceUpdatePlayer(player)
    }
    
    /**
     * Update scoreboard when player's health changes (from damage/healing)
     */
    fun onPlayerHealthChange(player: Player) {
        // Sync RPGPlayer health with Bukkit player health
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        rpgPlayer.health = player.health
        
        forceUpdatePlayer(player)
    }
    
    /**
     * Update scoreboard when player's health/mana changes
     */
    fun onPlayerHealthManaChange(player: Player) {
        forceUpdatePlayer(player)
    }
    
    /**
     * Update scoreboard when player's money changes
     */
    fun onPlayerMoneyChange(player: Player) {
        forceUpdatePlayer(player)
    }
    
    /**
     * Update scoreboard when player's class changes
     */
    fun onPlayerClassChange(player: Player) {
        forceUpdatePlayer(player)
    }
    
    /**
     * Update scoreboard when player's skills change
     */
    fun onPlayerSkillChange(player: Player) {
        forceUpdatePlayer(player)
    }
    
    /**
     * Update scoreboard when player's level changes
     */
    fun onPlayerLevelChange(player: Player) {
        forceUpdatePlayer(player)
    }
    
    /**
     * Update scoreboard when any player data changes
     */
    fun onPlayerDataChange(player: Player) {
        forceUpdatePlayer(player)
    }
}
