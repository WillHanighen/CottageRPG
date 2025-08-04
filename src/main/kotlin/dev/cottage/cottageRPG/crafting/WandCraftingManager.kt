package dev.cottage.cottageRPG.crafting

import dev.cottage.cottageRPG.CottageRPG
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.meta.ItemMeta

/**
 * Manages crafting recipes for magic wands and other RPG items
 */
class WandCraftingManager(private val plugin: CottageRPG) {

    fun registerWandRecipe() {
        val wandItem = createMagicWand()
        val key = NamespacedKey(plugin, "magic_wand")
        val recipe = ShapedRecipe(key, wandItem)

        // Get recipe pattern from config
        val recipePattern = plugin.configManager.getStringList("crafting.wand.pattern")
        val materialA = plugin.configManager.getString("crafting.wand.materials.a", "LAPIS_LAZULI")
        val materialB = plugin.configManager.getString("crafting.wand.materials.b", "STICK")

        // Set the recipe pattern (default: a b a, a b a, a b a)
        if (recipePattern.size >= 3) {
            recipe.shape(
                recipePattern[0],
                recipePattern[1], 
                recipePattern[2]
            )
        } else {
            // Fallback to default pattern
            recipe.shape(
                "aba",
                "aba", 
                "aba"
            )
        }

        // Set ingredients
        try {
            val matA = Material.valueOf(materialA.uppercase())
            val matB = Material.valueOf(materialB.uppercase())
            
            recipe.setIngredient('a', matA)
            recipe.setIngredient('b', matB)
            
            // Register the recipe
            plugin.server.addRecipe(recipe)
            
            if (plugin.configManager.getBoolean("general.debug", false)) {
                plugin.logger.info("Magic Wand recipe registered successfully")
                plugin.logger.info("Pattern: ${recipePattern.joinToString(", ")}")
                plugin.logger.info("Materials: a=$materialA, b=$materialB")
            }
        } catch (e: IllegalArgumentException) {
            plugin.logger.warning("Invalid material specified in wand recipe config: $materialA or $materialB")
            plugin.logger.warning("Using default materials: LAPIS_LAZULI and STICK")
            
            // Fallback to default materials
            recipe.setIngredient('a', Material.LAPIS_LAZULI)
            recipe.setIngredient('b', Material.STICK)
            plugin.server.addRecipe(recipe)
        }
    }

    private fun createMagicWand(): ItemStack {
        val wand = ItemStack(Material.STICK)
        val meta = wand.itemMeta
        
        meta?.let {
            // Get wand properties from config
            val wandName = plugin.configManager.getString("crafting.wand.name", "§5✦ Magic Wand ✦")
            val wandLore = plugin.configManager.getStringList("crafting.wand.lore")
            
            it.setDisplayName(wandName)
            
            val lore = if (wandLore.isNotEmpty()) {
                wandLore.map { line -> line.replace("&", "§") }
            } else {
                listOf(
                    "§7A mystical wand imbued with magical energy",
                    "",
                    "§6Right-click: §eCast primary spell",
                    "§6Shift + Right-click: §eCast secondary spell", 
                    "§6Left-click: §eSpecial/movement spell",
                    "§6Shift + Left-click: §eOpen spell selection"
                )
            }
            
            it.lore = lore
            
            // Add custom model data if specified in config
            val customModelData = plugin.configManager.getInt("crafting.wand.custom_model_data", -1)
            if (customModelData > 0) {
                it.setCustomModelData(customModelData)
            }
        }
        
        wand.itemMeta = meta
        return wand
    }

    fun unregisterRecipes() {
        try {
            val key = NamespacedKey(plugin, "magic_wand")
            plugin.server.removeRecipe(key)
        } catch (e: Exception) {
            plugin.logger.warning("Error unregistering wand recipe: ${e.message}")
        }
    }

    /**
     * Check if an item is a magic wand
     */
    fun isMagicWand(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.STICK) return false
        
        val meta = item.itemMeta ?: return false
        val displayName = meta.displayName ?: return false
        val lore = meta.lore ?: return false
        
        // Case-insensitive check for "wand" in both display name and lore
        val hasWandInName = displayName.lowercase().contains("wand")
        val hasWandInLore = lore.any { it.lowercase().contains("wand") }
        
        return hasWandInName && hasWandInLore
    }
}
