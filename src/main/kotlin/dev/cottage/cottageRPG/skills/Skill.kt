package dev.cottage.cottageRPG.skills

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Represents a skill that players can level up using PaperMC APIs
 */
data class Skill(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: Material,
    val category: SkillCategory,
    val maxLevel: Int = 100
) {
    
    /**
     * Get the skill icon as an ItemStack with Adventure Components
     */
    fun getIcon(currentLevel: Int, currentExp: Long, expForNext: Long): ItemStack {
        val item = ItemStack(icon)
        val meta = item.itemMeta
        meta?.let {
            // Set display name using Adventure Components
            it.displayName(Component.text(displayName, NamedTextColor.GOLD, TextDecoration.BOLD))
            
            // Create lore using Adventure Components
            val lore = listOf(
                Component.text(description, NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Level: ", NamedTextColor.YELLOW)
                    .append(Component.text("$currentLevel", NamedTextColor.WHITE))
                    .append(Component.text("/", NamedTextColor.GRAY))
                    .append(Component.text("$maxLevel", NamedTextColor.WHITE)),
                Component.text("Progress: ", NamedTextColor.YELLOW)
                    .append(Component.text("$currentExp", NamedTextColor.WHITE))
                    .append(Component.text("/", NamedTextColor.GRAY))
                    .append(Component.text("$expForNext", NamedTextColor.WHITE)),
                Component.text("Category: ", NamedTextColor.YELLOW)
                    .append(Component.text(category.displayName, category.getColor()))
            )
            it.lore(lore)
        }
        item.itemMeta = meta
        return item
    }
    
    /**
     * Get a detailed skill icon with additional information
     */
    fun getDetailedIcon(currentLevel: Int, currentExp: Long, expForNext: Long, totalExp: Long): ItemStack {
        val item = ItemStack(icon)
        val meta = item.itemMeta
        meta?.let {
            // Set display name using Adventure Components
            it.displayName(Component.text(displayName, NamedTextColor.GOLD, TextDecoration.BOLD))
            
            // Calculate progress percentage
            val progressPercent = if (expForNext > 0) {
                ((currentExp.toDouble() / expForNext.toDouble()) * 100).toInt()
            } else 100
            
            // Create detailed lore using Adventure Components
            val lore = listOf(
                Component.text(description, NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Level: ", NamedTextColor.YELLOW)
                    .append(Component.text("$currentLevel", NamedTextColor.WHITE))
                    .append(Component.text("/", NamedTextColor.GRAY))
                    .append(Component.text("$maxLevel", NamedTextColor.WHITE)),
                Component.text("Progress: ", NamedTextColor.YELLOW)
                    .append(Component.text("$currentExp", NamedTextColor.WHITE))
                    .append(Component.text("/", NamedTextColor.GRAY))
                    .append(Component.text("$expForNext", NamedTextColor.WHITE))
                    .append(Component.text(" ($progressPercent%)", NamedTextColor.GREEN)),
                Component.text("Total Experience: ", NamedTextColor.YELLOW)
                    .append(Component.text("$totalExp", NamedTextColor.WHITE)),
                Component.text("Category: ", NamedTextColor.YELLOW)
                    .append(Component.text(category.displayName, category.getColor())),
                Component.empty(),
                Component.text("Click to view skill details!", NamedTextColor.AQUA, TextDecoration.ITALIC)
            )
            it.lore(lore)
        }
        item.itemMeta = meta
        return item
    }
    
    companion object {
        /**
         * Get all default skills
         */
        fun getDefaultSkills(): List<Skill> {
            return listOf(
                Skill(
                    id = "mining",
                    displayName = "Mining",
                    description = "Skill in extracting ores and stones",
                    icon = Material.IRON_PICKAXE,
                    category = SkillCategory.GATHERING
                ),
                Skill(
                    id = "woodcutting",
                    displayName = "Woodcutting",
                    description = "Skill in chopping down trees",
                    icon = Material.IRON_AXE,
                    category = SkillCategory.GATHERING
                ),
                Skill(
                    id = "farming",
                    displayName = "Farming",
                    description = "Skill in growing crops and tending animals",
                    icon = Material.IRON_HOE,
                    category = SkillCategory.GATHERING
                ),
                Skill(
                    id = "combat",
                    displayName = "Combat",
                    description = "Skill in melee fighting",
                    icon = Material.IRON_SWORD,
                    category = SkillCategory.COMBAT
                ),
                Skill(
                    id = "archery",
                    displayName = "Archery",
                    description = "Skill in ranged combat with bows",
                    icon = Material.BOW,
                    category = SkillCategory.COMBAT
                ),
                Skill(
                    id = "magic",
                    displayName = "Magic",
                    description = "Skill in casting spells and enchantments",
                    icon = Material.ENCHANTED_BOOK,
                    category = SkillCategory.MAGIC
                ),
                Skill(
                    id = "fishing",
                    displayName = "Fishing",
                    description = "Skill in catching fish and sea creatures",
                    icon = Material.FISHING_ROD,
                    category = SkillCategory.GATHERING
                ),
                Skill(
                    id = "cooking",
                    displayName = "Cooking",
                    description = "Skill in preparing food and potions",
                    icon = Material.FURNACE,
                    category = SkillCategory.CRAFTING
                ),
                Skill(
                    id = "smithing",
                    displayName = "Smithing",
                    description = "Skill in crafting weapons and armor",
                    icon = Material.ANVIL,
                    category = SkillCategory.CRAFTING
                ),
                Skill(
                    id = "alchemy",
                    displayName = "Alchemy",
                    description = "Skill in brewing potions and elixirs",
                    icon = Material.BREWING_STAND,
                    category = SkillCategory.MAGIC
                ),
                Skill(
                    id = "enchanting",
                    displayName = "Enchanting",
                    description = "Skill in imbuing items with magical properties",
                    icon = Material.ENCHANTING_TABLE,
                    category = SkillCategory.CRAFTING
                ),
                Skill(
                    id = "runecrafting",
                    displayName = "Runecrafting",
                    description = "Skill in creating and inscribing magical runes",
                    icon = Material.CARVED_PUMPKIN,
                    category = SkillCategory.MAGIC
                )
            )
        }
    }
}

/**
 * Categories for organizing skills with PaperMC Adventure Component support
 */
enum class SkillCategory(val displayName: String, private val colorCode: String) {
    GATHERING("Gathering", "§2"),
    COMBAT("Combat", "§c"),
    MAGIC("Magic", "§5"),
    CRAFTING("Crafting", "§6");
    
    /**
     * Get the category color as a NamedTextColor for Adventure Components
     */
    fun getColor(): NamedTextColor {
        return when (this) {
            GATHERING -> NamedTextColor.DARK_GREEN
            COMBAT -> NamedTextColor.RED
            MAGIC -> NamedTextColor.DARK_PURPLE
            CRAFTING -> NamedTextColor.GOLD
        }
    }
    
    /**
     * Get the category color as a Component for display
     */
    fun getColoredName(): Component {
        return Component.text(displayName, getColor())
    }
}
