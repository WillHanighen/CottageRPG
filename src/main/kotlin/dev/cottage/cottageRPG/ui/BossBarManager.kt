package dev.cottage.cottageRPG.ui

import dev.cottage.cottageRPG.CottageRPG
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages boss bars for XP progress display using PaperMC APIs
 */
class BossBarManager(private val plugin: CottageRPG) {
    
    private val activeBossBars = ConcurrentHashMap<Player, BossBar>()
    private val bossBarTasks = ConcurrentHashMap<Player, BukkitTask>()
    
    /**
     * Show XP progress boss bar for a skill using Adventure Components
     */
    fun showSkillXpBar(player: Player, skillName: String, xpGained: Long) {
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val skillLevel = rpgPlayer.getSkillLevel(skillName)
        val skillExp = rpgPlayer.skillExperience[skillName] ?: 0L
        
        // Get experience needed for next level from current level
        val expNeededForNextLevel = rpgPlayer.getSkillExperienceForNextLevel(skillLevel)
        
        // The skillExperience map stores progress within the current level (resets on level up)
        // So we can directly calculate progress as: current exp / exp needed for next level
        val progress = if (expNeededForNextLevel > 0) {
            (skillExp.toDouble() / expNeededForNextLevel.toDouble()).coerceIn(0.0, 1.0)
        } else {
            1.0 // Max level or edge case
        }
        
        // Remove existing boss bar if present
        removeBossBar(player)
        
        // Create new boss bar with Adventure Components (converted to legacy string for boss bar API)
        val titleComponent = Component.text(skillName.replaceFirstChar { it.uppercase() }, NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text(" Level ", NamedTextColor.GRAY))
            .append(Component.text(skillLevel.toString(), NamedTextColor.GOLD))
            .append(Component.text(" (+", NamedTextColor.GRAY))
            .append(Component.text(xpGained.toString(), NamedTextColor.GREEN))
            .append(Component.text(" XP)", NamedTextColor.GRAY))
        
        // Convert Component to legacy string for boss bar creation
        val title = "§6§l${skillName.replaceFirstChar { it.uppercase() }} §7Level §6$skillLevel §7(+§a$xpGained§7 XP)"
        val bossBar = plugin.server.createBossBar(title, getBarColor(skillName), BarStyle.SOLID)
        bossBar.progress = progress
        bossBar.addPlayer(player)
        
        activeBossBars[player] = bossBar
        
        // Schedule removal after 5 seconds using PaperMC scheduler
        val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
            removeBossBar(player)
        }, 100L) // 5 seconds = 100 ticks
        
        bossBarTasks[player] = task
    }
    
    /**
     * Show main level XP progress boss bar using Adventure Components
     */
    fun showMainLevelXpBar(player: Player, xpGained: Long) {
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val level = rpgPlayer.level
        val experience = rpgPlayer.experience
        
        // Calculate progress to next level
        val expForNext = rpgPlayer.getExperienceForNextLevel()
        val currentLevelExp = experience % expForNext
        val progress = (currentLevelExp.toDouble() / expForNext.toDouble()).coerceIn(0.0, 1.0)
        
        // Remove existing boss bar if present
        removeBossBar(player)
        
        // Create new boss bar with Adventure Components (converted to legacy string for boss bar API)
        val titleComponent = Component.text("Level ", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text(level.toString(), NamedTextColor.YELLOW))
            .append(Component.text(" (+", NamedTextColor.GRAY))
            .append(Component.text(xpGained.toString(), NamedTextColor.GREEN))
            .append(Component.text(" XP)", NamedTextColor.GRAY))
        
        // Convert Component to legacy string for boss bar creation
        val title = "§6§lLevel §e$level §7(+§a$xpGained§7 XP)"
        val bossBar = plugin.server.createBossBar(title, BarColor.YELLOW, BarStyle.SOLID)
        bossBar.progress = progress
        bossBar.addPlayer(player)
        
        activeBossBars[player] = bossBar
        
        // Schedule removal after 5 seconds using PaperMC scheduler
        val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
            removeBossBar(player)
        }, 100L) // 5 seconds = 100 ticks
        
        bossBarTasks[player] = task
    }
    
    /**
     * Remove boss bar for a player
     */
    fun removeBossBar(player: Player) {
        activeBossBars[player]?.let { bossBar ->
            bossBar.removeAll()
            activeBossBars.remove(player)
        }
        
        bossBarTasks[player]?.let { task ->
            task.cancel()
            bossBarTasks.remove(player)
        }
    }
    
    /**
     * Remove all boss bars (for plugin shutdown)
     */
    fun removeAllBossBars() {
        activeBossBars.values.forEach { bossBar ->
            bossBar.removeAll()
        }
        activeBossBars.clear()
        
        bossBarTasks.values.forEach { task ->
            task.cancel()
        }
        bossBarTasks.clear()
    }
    
    /**
     * Get bar color based on skill type
     */
    private fun getBarColor(skillName: String): BarColor {
        return when (skillName.lowercase()) {
            "mining" -> BarColor.BLUE
            "woodcutting" -> BarColor.GREEN
            "farming" -> BarColor.GREEN
            "fishing" -> BarColor.BLUE
            "combat" -> BarColor.RED
            "archery" -> BarColor.PURPLE
            "smithing" -> BarColor.WHITE
            "alchemy" -> BarColor.PINK
            "magic" -> BarColor.PURPLE
            else -> BarColor.YELLOW
        }
    }
    
    /**
     * Create a temporary informational boss bar with Adventure Components
     */
    fun showInfoBar(player: Player, message: Component, color: BarColor = BarColor.BLUE, durationTicks: Long = 60L) {
        // Remove existing boss bar if present
        removeBossBar(player)
        
        // Convert Component to legacy string for boss bar creation
        val legacyMessage = when {
            message.children().isEmpty() -> {
                // Simple text component
                val content = (message as? net.kyori.adventure.text.TextComponent)?.content() ?: message.toString()
                "§f$content"
            }
            else -> {
                // Complex component - use simple string conversion
                message.toString()
            }
        }
        
        val bossBar = plugin.server.createBossBar(legacyMessage, color, BarStyle.SOLID)
        bossBar.progress = 1.0
        bossBar.addPlayer(player)
        
        activeBossBars[player] = bossBar
        
        // Schedule removal after specified duration
        val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
            removeBossBar(player)
        }, durationTicks)
        
        bossBarTasks[player] = task
    }
    
    /**
     * Create a temporary informational boss bar with string message (convenience method)
     */
    fun showInfoBar(player: Player, message: String, color: BarColor = BarColor.BLUE, durationTicks: Long = 60L) {
        // Remove existing boss bar if present
        removeBossBar(player)
        
        val bossBar = plugin.server.createBossBar("§f$message", color, BarStyle.SOLID)
        bossBar.progress = 1.0
        bossBar.addPlayer(player)
        
        activeBossBars[player] = bossBar
        
        // Schedule removal after specified duration
        val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
            removeBossBar(player)
        }, durationTicks)
        
        bossBarTasks[player] = task
    }
    
    /**
     * Calculate experience required for a specific level
     */
    private fun getExperienceForLevel(level: Int): Long {
        if (level <= 1) return 0L
        return (100L * level * level - 100L * level).toLong()
    }
}
