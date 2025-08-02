package dev.cottage.cottageRPG.player

import dev.cottage.cottageRPG.classes.RPGClass
import dev.cottage.cottageRPG.skills.Skill
import org.bukkit.entity.Player
import java.util.*
import kotlin.collections.HashMap

/**
 * Represents an RPG player with stats, skills, and class information
 */
data class RPGPlayer(
    val uuid: UUID,
    var level: Int = 1,
    var experience: Long = 0,
    var health: Double = 20.0,
    var maxHealth: Double = 20.0,
    var mana: Double = 100.0,
    var maxMana: Double = 100.0,
    var rpgClass: RPGClass? = null,
    val skills: MutableMap<String, Int> = HashMap(),
    val skillExperience: MutableMap<String, Long> = HashMap(),
    var money: Double = 0.0,
    var lastSeen: Long = System.currentTimeMillis(),
    var isOnline: Boolean = false
) {
    
    /**
     * Get the Bukkit player object if online
     */
    fun getBukkitPlayer(): Player? {
        return org.bukkit.Bukkit.getPlayer(uuid)
    }
    
    /**
     * Calculate experience needed for next level
     */
    fun getExperienceForNextLevel(): Long {
        return (level * 100L * 1.2).toLong()
    }
    
    /**
     * Calculate total experience needed for a specific level
     */
    fun getTotalExperienceForLevel(targetLevel: Int): Long {
        var total = 0L
        for (i in 1 until targetLevel) {
            total += (i * 100L * 1.2).toLong()
        }
        return total
    }
    
    /**
     * Add experience and handle level ups
     */
    fun addExperience(amount: Long): Boolean {
        experience += amount
        val expNeeded = getExperienceForNextLevel()
        
        if (experience >= expNeeded) {
            levelUp()
            return true
        }
        return false
    }
    
    /**
     * Level up the player
     */
    private fun levelUp() {
        val expNeeded = getExperienceForNextLevel()
        experience -= expNeeded
        level++
        
        // Increase stats based on class
        rpgClass?.let { playerClass ->
            maxHealth += playerClass.healthPerLevel
            maxMana += playerClass.manaPerLevel
            health = maxHealth // Full heal on level up
            mana = maxMana // Full mana on level up
        } ?: run {
            // Default stat increases if no class
            maxHealth += 2.0
            maxMana += 10.0
        }
        
        // Update Bukkit player's max health if online
        getBukkitPlayer()?.let { player ->
            player.maxHealth = maxHealth
            player.sendMessage("§6Congratulations! You reached level $level!")
            player.playSound(player.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
        }
    }
    
    /**
     * Get skill level
     */
    fun getSkillLevel(skillName: String): Int {
        return skills[skillName] ?: 0
    }
    
    /**
     * Set skill level
     */
    fun setSkillLevel(skillName: String, level: Int) {
        skills[skillName] = level
    }
    
    /**
     * Add skill experience
     */
    fun addSkillExperience(skillName: String, amount: Long): Boolean {
        val currentExp = skillExperience[skillName] ?: 0L
        skillExperience[skillName] = currentExp + amount
        
        val currentLevel = getSkillLevel(skillName)
        val expNeeded = getSkillExperienceForNextLevel(currentLevel)
        
        if (currentExp + amount >= expNeeded && currentLevel < 100) {
            setSkillLevel(skillName, currentLevel + 1)
            skillExperience[skillName] = (currentExp + amount) - expNeeded
            
            // Notify player if online
            getBukkitPlayer()?.let { player ->
                player.sendMessage("§6Your $skillName skill increased to level ${currentLevel + 1}!")
                player.playSound(player.location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
            }
            return true
        }
        return false
    }
    
    /**
     * Calculate experience needed for next skill level
     */
    fun getSkillExperienceForNextLevel(currentLevel: Int): Long {
        return (currentLevel * 50L * 1.1).toLong()
    }
    
    /**
     * Set the player's class
     */
    fun setClass(newClass: RPGClass) {
        rpgClass = newClass
        
        // Apply class bonuses
        maxHealth = 20.0 + (level - 1) * newClass.healthPerLevel
        maxMana = 100.0 + (level - 1) * newClass.manaPerLevel
        health = maxHealth
        mana = maxMana
        
        getBukkitPlayer()?.let { player ->
            player.sendMessage("§6You are now a ${newClass.displayName}!")
        }
    }
    
    /**
     * Regenerate mana over time
     */
    fun regenerateMana(amount: Double) {
        mana = (mana + amount).coerceAtMost(maxMana)
    }
    
    /**
     * Use mana for abilities
     */
    fun useMana(amount: Double): Boolean {
        return if (mana >= amount) {
            mana -= amount
            true
        } else {
            false
        }
    }
    
    /**
     * Add money to the player
     */
    fun addMoney(amount: Double) {
        money += amount
    }
    
    /**
     * Remove money from the player
     */
    fun removeMoney(amount: Double): Boolean {
        return if (money >= amount) {
            money -= amount
            true
        } else {
            false
        }
    }
    
    /**
     * Update last seen timestamp
     */
    fun updateLastSeen() {
        lastSeen = System.currentTimeMillis()
    }
    
    /**
     * Get total experience gained for a specific skill (including levels)
     */
    fun getTotalSkillExperience(skillId: String): Long {
        val currentLevel = getSkillLevel(skillId)
        val currentExp = skillExperience[skillId] ?: 0L
        
        // Calculate total experience from all previous levels
        var totalExp = 0L
        for (level in 1 until currentLevel) {
            totalExp += getSkillExperienceForLevel(level)
        }
        
        // Add current level progress
        totalExp += currentExp
        
        return totalExp
    }
    
    /**
     * Calculate experience required for a specific skill level
     */
    private fun getSkillExperienceForLevel(level: Int): Long {
        return (level * 50L * 1.1).toLong()
    }
}
