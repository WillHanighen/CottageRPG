package dev.cottage.cottageRPG.spells

import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * Represents a spell that can players can cast
 */
data class Spell(
    val id: String,
    val name: String,
    val description: String,
    val effect: String,
    val icon: Material,
    val category: SpellCategory,
    val requiredMagicLevel: Int,
    val manaCost: Double,
    val cooldownSeconds: Int
) {
    
    /**
     * Check if a player can cast this spell
     */
    fun canCast(player: Player, playerMagicLevel: Int, playerMana: Double, cooldownManager: SpellCooldownManager? = null): SpellCastResult {
        if (playerMagicLevel < requiredMagicLevel) {
            return SpellCastResult.INSUFFICIENT_LEVEL
        }
        
        if (playerMana < manaCost) {
            return SpellCastResult.INSUFFICIENT_MANA
        }
        
        // Check cooldown if manager is provided
        if (cooldownManager != null && cooldownManager.isOnCooldown(player, id)) {
            return SpellCastResult.ON_COOLDOWN
        }
        
        return SpellCastResult.SUCCESS
    }
    
    /**
     * Get formatted spell information for display
     */
    fun getFormattedInfo(playerMagicLevel: Int, playerMana: Double): List<String> {
        val info = mutableListOf<String>()
        info.add("§d${name}")
        info.add("§7${description}")
        info.add("")
        info.add("§6Category: §f${category.displayName}")
        info.add("§6Required Level: §f${requiredMagicLevel}")
        info.add("§6Mana Cost: §f${manaCost}")
        info.add("§6Cooldown: §f${cooldownSeconds}s")
        info.add("")
        
        // Add status indicators
        if (playerMagicLevel < requiredMagicLevel) {
            info.add("§c✗ Level too low!")
        } else {
            info.add("§a✓ Level requirement met")
        }
        
        if (playerMana < manaCost) {
            info.add("§c✗ Not enough mana!")
        } else {
            info.add("§a✓ Sufficient mana")
        }
        
        info.add("")
        info.add("§7Effect: §f${effect}")
        
        return info
    }
    
    companion object {
        /**
         * Get all available spells organized by category
         */
        fun getAllSpells(): Map<SpellCategory, List<Spell>> {
            return mapOf(
                SpellCategory.PRIMARY to getPrimarySpells(),
                SpellCategory.SECONDARY to getSecondarySpells(),
                SpellCategory.SPECIAL_MOVEMENT to getSpecialMovementSpells()
            )
        }
        
        private fun getPrimarySpells(): List<Spell> {
            return listOf(
                Spell(
                    id = "fireball",
                    name = "Fireball",
                    description = "Launch a blazing fireball",
                    effect = "Deals fire damage to target",
                    icon = Material.FIRE_CHARGE,
                    category = SpellCategory.PRIMARY,
                    requiredMagicLevel = 0,
                    manaCost = 15.0,
                    cooldownSeconds = 3
                ),
                Spell(
                    id = "lightning_bolt",
                    name = "Lightning Bolt",
                    description = "Strike with lightning",
                    effect = "Deals electric damage",
                    icon = Material.BLAZE_ROD,
                    category = SpellCategory.PRIMARY,
                    requiredMagicLevel = 15,
                    manaCost = 25.0,
                    cooldownSeconds = 5
                ),
                Spell(
                    id = "ice_shard",
                    name = "Ice Shard",
                    description = "Fire sharp ice projectiles",
                    effect = "Slows and damages target",
                    icon = Material.ICE,
                    category = SpellCategory.PRIMARY,
                    requiredMagicLevel = 8,
                    manaCost = 20.0,
                    cooldownSeconds = 4
                ),
                Spell(
                    id = "magic_missile",
                    name = "Magic Missile",
                    description = "Basic magical projectile",
                    effect = "Reliable magical damage",
                    icon = Material.ARROW,
                    category = SpellCategory.PRIMARY,
                    requiredMagicLevel = 1,
                    manaCost = 10.0,
                    cooldownSeconds = 2
                )
            )
        }
        
        private fun getSecondarySpells(): List<Spell> {
            return listOf(
                Spell(
                    id = "shield",
                    name = "Shield",
                    description = "Create magical protection",
                    effect = "Absorbs incoming damage",
                    icon = Material.SHIELD,
                    category = SpellCategory.SECONDARY,
                    requiredMagicLevel = 5,
                    manaCost = 30.0,
                    cooldownSeconds = 10
                ),
                Spell(
                    id = "heal",
                    name = "Heal",
                    description = "Restore health",
                    effect = "Heals the caster",
                    icon = Material.GOLDEN_APPLE,
                    category = SpellCategory.SECONDARY,
                    requiredMagicLevel = 3,
                    manaCost = 25.0,
                    cooldownSeconds = 8
                ),
                Spell(
                    id = "barrier",
                    name = "Barrier",
                    description = "Create protective wall",
                    effect = "Blocks projectiles",
                    icon = Material.OBSIDIAN,
                    category = SpellCategory.SECONDARY,
                    requiredMagicLevel = 20,
                    manaCost = 40.0,
                    cooldownSeconds = 15
                ),
                Spell(
                    id = "mana_shield",
                    name = "Mana Shield",
                    description = "Create a magical barrier that converts damage to mana loss",
                    effect = "Absorbs damage using mana (2 mana per 1 damage) for 20 seconds",
                    icon = Material.ENCHANTED_BOOK,
                    category = SpellCategory.SECONDARY,
                    requiredMagicLevel = 12,
                    manaCost = 20.0,
                    cooldownSeconds = 12
                )
            )
        }
        
        private fun getSpecialMovementSpells(): List<Spell> {
            return listOf(
                Spell(
                    id = "light",
                    name = "Light",
                    description = "Create magical light",
                    effect = "Illuminates dark areas",
                    icon = Material.TORCH,
                    category = SpellCategory.SPECIAL_MOVEMENT,
                    requiredMagicLevel = 0,
                    manaCost = 5.0,
                    cooldownSeconds = 1
                ),
                Spell(
                    id = "teleport",
                    name = "Teleport",
                    description = "Instant transportation",
                    effect = "Teleport to target location",
                    icon = Material.ENDER_PEARL,
                    category = SpellCategory.SPECIAL_MOVEMENT,
                    requiredMagicLevel = 25,
                    manaCost = 50.0,
                    cooldownSeconds = 20
                ),
                Spell(
                    id = "invisibility",
                    name = "Invisibility",
                    description = "Become invisible",
                    effect = "Temporary invisibility",
                    icon = Material.GLASS,
                    category = SpellCategory.SPECIAL_MOVEMENT,
                    requiredMagicLevel = 18,
                    manaCost = 35.0,
                    cooldownSeconds = 30
                ),
                Spell(
                    id = "jump_boost",
                    name = "Jump Boost",
                    description = "Enhanced jumping",
                    effect = "Temporary jump boost",
                    icon = Material.FEATHER,
                    category = SpellCategory.SPECIAL_MOVEMENT,
                    requiredMagicLevel = 10,
                    manaCost = 15.0,
                    cooldownSeconds = 5
                ),
                Spell(
                    id = "speed",
                    name = "Speed",
                    description = "Increased movement",
                    effect = "Temporary speed boost",
                    icon = Material.SUGAR,
                    category = SpellCategory.SPECIAL_MOVEMENT,
                    requiredMagicLevel = 5,
                    manaCost = 12.0,
                    cooldownSeconds = 12
                ),
                Spell(
                    id = "levitation",
                    name = "Levitation",
                    description = "Float in the air",
                    effect = "Temporary levitation",
                    icon = Material.SHULKER_SHELL,
                    category = SpellCategory.SPECIAL_MOVEMENT,
                    requiredMagicLevel = 20,
                    manaCost = 30.0,
                    cooldownSeconds = 15
                ),
                Spell(
                    id = "dash",
                    name = "Dash",
                    description = "Quick forward movement",
                    effect = "Instant forward dash",
                    icon = Material.RABBIT_FOOT,
                    category = SpellCategory.SPECIAL_MOVEMENT,
                    requiredMagicLevel = 8,
                    manaCost = 10.0,
                    cooldownSeconds = 4
                )
            )
        }
    }
}

/**
 * Categories for spell organization
 */
enum class SpellCategory(val displayName: String, val description: String) {
    PRIMARY("Primary", "Main offensive spells"),
    SECONDARY("Secondary", "Defensive and support spells"),
    SPECIAL_MOVEMENT("Special/Movement", "Utility and movement spells")
}

/**
 * Result of attempting to cast a spell
 */
enum class SpellCastResult {
    SUCCESS,
    INSUFFICIENT_LEVEL,
    INSUFFICIENT_MANA,
    ON_COOLDOWN,
    FAILED
}
