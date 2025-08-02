package dev.cottage.cottageRPG.classes

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Represents an RPG class with unique stats and abilities
 */
data class RPGClass(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: Material,
    val healthPerLevel: Double,
    val manaPerLevel: Double,
    val primaryStat: String,
    val secondaryStat: String,
    val abilities: List<String> = emptyList(),
    val requirements: Map<String, Int> = emptyMap()
) {
    
    /**
     * Get the icon as an ItemStack with proper display name and lore
     */
    fun getIcon(): ItemStack {
        val item = ItemStack(icon)
        val meta = item.itemMeta
        meta?.let {
            it.setDisplayName("§6$displayName")
            it.lore = listOf(
                "§7$description",
                "",
                "§eStats per level:",
                "§c❤ Health: +$healthPerLevel",
                "§9✦ Mana: +$manaPerLevel",
                "",
                "§ePrimary Stat: §f$primaryStat",
                "§eSecondary Stat: §f$secondaryStat"
            )
        }
        item.itemMeta = meta
        return item
    }
    
    /**
     * Check if a player meets the requirements for this class
     */
    fun meetsRequirements(playerSkills: Map<String, Int>): Boolean {
        return requirements.all { (skill, requiredLevel) ->
            (playerSkills[skill] ?: 0) >= requiredLevel
        }
    }
    
    companion object {
        /**
         * Get all default RPG classes
         */
        fun getDefaultClasses(): List<RPGClass> {
            return listOf(
                RPGClass(
                    id = "warrior",
                    displayName = "Warrior",
                    description = "A mighty fighter skilled in melee combat",
                    icon = Material.IRON_SWORD,
                    healthPerLevel = 3.0,
                    manaPerLevel = 5.0,
                    primaryStat = "Strength",
                    secondaryStat = "Constitution",
                    abilities = listOf("charge", "berserker_rage", "shield_bash"),
                    requirements = mapOf("combat" to 10)
                ),
                RPGClass(
                    id = "mage",
                    displayName = "Mage",
                    description = "A master of arcane magic and spells",
                    icon = Material.BLAZE_ROD,
                    healthPerLevel = 1.0,
                    manaPerLevel = 15.0,
                    primaryStat = "Intelligence",
                    secondaryStat = "Wisdom",
                    abilities = listOf("fireball", "ice_shard", "lightning_bolt"),
                    requirements = mapOf("magic" to 15)
                ),
                RPGClass(
                    id = "archer",
                    displayName = "Archer",
                    description = "A skilled marksman with bow and arrow",
                    icon = Material.BOW,
                    healthPerLevel = 2.0,
                    manaPerLevel = 8.0,
                    primaryStat = "Dexterity",
                    secondaryStat = "Perception",
                    abilities = listOf("power_shot", "multi_shot", "explosive_arrow"),
                    requirements = mapOf("archery" to 20)
                ),
                RPGClass(
                    id = "rogue",
                    displayName = "Rogue",
                    description = "A stealthy assassin who strikes from the shadows",
                    icon = Material.IRON_SWORD,
                    healthPerLevel = 2.5,
                    manaPerLevel = 10.0,
                    primaryStat = "Dexterity",
                    secondaryStat = "Intelligence",
                    abilities = listOf("stealth", "backstab", "poison_blade"),
                    requirements = mapOf("combat" to 5, "mining" to 10)
                )
            )
        }
    }
}
