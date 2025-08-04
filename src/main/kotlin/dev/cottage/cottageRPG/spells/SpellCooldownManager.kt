package dev.cottage.cottageRPG.spells

import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages spell cooldowns for players
 */
class SpellCooldownManager {
    
    private val playerCooldowns = ConcurrentHashMap<UUID, MutableMap<String, Long>>()
    
    /**
     * Check if a spell is on cooldown for a player
     */
    fun isOnCooldown(player: Player, spellId: String): Boolean {
        val playerUUID = player.uniqueId
        val cooldowns = playerCooldowns[playerUUID] ?: return false
        val cooldownEnd = cooldowns[spellId] ?: return false
        
        return System.currentTimeMillis() < cooldownEnd
    }
    
    /**
     * Get remaining cooldown time in seconds
     */
    fun getRemainingCooldown(player: Player, spellId: String): Int {
        val playerUUID = player.uniqueId
        val cooldowns = playerCooldowns[playerUUID] ?: return 0
        val cooldownEnd = cooldowns[spellId] ?: return 0
        
        val remaining = (cooldownEnd - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining.toInt() else 0
    }
    
    /**
     * Set a spell on cooldown for a player
     */
    fun setCooldown(player: Player, spellId: String, cooldownSeconds: Int) {
        val playerUUID = player.uniqueId
        val cooldowns = playerCooldowns.computeIfAbsent(playerUUID) { mutableMapOf() }
        val cooldownEnd = System.currentTimeMillis() + (cooldownSeconds * 1000)
        cooldowns[spellId] = cooldownEnd
    }
    
    /**
     * Clear all cooldowns for a player (useful for admin commands or testing)
     */
    fun clearCooldowns(player: Player) {
        playerCooldowns.remove(player.uniqueId)
    }
    
    /**
     * Clear a specific spell cooldown for a player
     */
    fun clearCooldown(player: Player, spellId: String) {
        val playerUUID = player.uniqueId
        playerCooldowns[playerUUID]?.remove(spellId)
    }
    
    /**
     * Clean up expired cooldowns (should be called periodically)
     */
    fun cleanupExpiredCooldowns() {
        val currentTime = System.currentTimeMillis()
        
        playerCooldowns.values.forEach { cooldowns ->
            cooldowns.entries.removeIf { (_, cooldownEnd) ->
                currentTime >= cooldownEnd
            }
        }
        
        // Remove empty player entries
        playerCooldowns.entries.removeIf { (_, cooldowns) ->
            cooldowns.isEmpty()
        }
    }
    
    /**
     * Get all active cooldowns for a player
     */
    fun getActiveCooldowns(player: Player): Map<String, Int> {
        val playerUUID = player.uniqueId
        val cooldowns = playerCooldowns[playerUUID] ?: return emptyMap()
        val currentTime = System.currentTimeMillis()
        
        return cooldowns.mapNotNull { (spellId, cooldownEnd) ->
            val remaining = ((cooldownEnd - currentTime) / 1000).toInt()
            if (remaining > 0) spellId to remaining else null
        }.toMap()
    }
    
    /**
     * Reduce all cooldowns for a player by a certain amount (useful for items/effects)
     */
    fun reduceCooldowns(player: Player, reductionSeconds: Int) {
        val playerUUID = player.uniqueId
        val cooldowns = playerCooldowns[playerUUID] ?: return
        val reductionMs = reductionSeconds * 1000L
        
        cooldowns.replaceAll { _, cooldownEnd ->
            (cooldownEnd - reductionMs).coerceAtLeast(System.currentTimeMillis())
        }
    }
    
    /**
     * Apply cooldown reduction based on magic level (higher level = shorter cooldowns)
     */
    fun setCooldownWithReduction(player: Player, spellId: String, baseCooldownSeconds: Int, magicLevel: Int) {
        // Reduce cooldown by 1% per magic level, max 50% reduction at level 50
        val reductionPercent = (magicLevel * 0.01).coerceAtMost(0.5)
        val actualCooldown = (baseCooldownSeconds * (1.0 - reductionPercent)).toInt().coerceAtLeast(1)
        
        setCooldown(player, spellId, actualCooldown)
    }
    
    /**
     * Get formatted cooldown display for a player
     */
    fun getCooldownDisplay(player: Player): List<String> {
        val activeCooldowns = getActiveCooldowns(player)
        if (activeCooldowns.isEmpty()) {
            return listOf("§7No active spell cooldowns")
        }
        
        val display = mutableListOf<String>()
        display.add("§5✦ Active Spell Cooldowns ✦")
        display.add("")
        
        // Get spell names for display
        val allSpells = dev.cottage.cottageRPG.spells.Spell.getAllSpells().values.flatten().associateBy { it.id }
        
        activeCooldowns.forEach { (spellId, remaining) ->
            val spellName = allSpells[spellId]?.name ?: spellId
            val minutes = remaining / 60
            val seconds = remaining % 60
            
            val timeDisplay = if (minutes > 0) {
                "${minutes}m ${seconds}s"
            } else {
                "${seconds}s"
            }
            
            display.add("§6${spellName}: §f${timeDisplay}")
        }
        
        return display
    }
    
    /**
     * Check if player has any cooldowns active
     */
    fun hasActiveCooldowns(player: Player): Boolean {
        return getActiveCooldowns(player).isNotEmpty()
    }
}
