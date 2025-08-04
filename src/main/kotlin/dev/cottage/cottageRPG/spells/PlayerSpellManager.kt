package dev.cottage.cottageRPG.spells

import dev.cottage.cottageRPG.CottageRPG
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Manages selected spells for each player with persistent storage
 */
class PlayerSpellManager(private val plugin: CottageRPG) {
    
    private val playerSelectedSpells = ConcurrentHashMap<UUID, MutableMap<SpellCategory, String>>()
    private val dataFile = File(plugin.dataFolder, "spell-selections.yml")
    private var config: FileConfiguration = YamlConfiguration()
    
    init {
        loadSpellSelections()
    }
    
    /**
     * Load spell selections from file
     */
    private fun loadSpellSelections() {
        try {
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }
            
            if (!dataFile.exists()) {
                dataFile.createNewFile()
                config = YamlConfiguration()
                saveSpellSelections()
                return
            }
            
            config = YamlConfiguration.loadConfiguration(dataFile)
            
            // Load all player spell selections
            config.getKeys(false).forEach { uuidString ->
                try {
                    val playerUUID = UUID.fromString(uuidString)
                    val playerSelections = mutableMapOf<SpellCategory, String>()
                    
                    SpellCategory.values().forEach { category ->
                        val spellId = config.getString("$uuidString.${category.name}")
                        if (spellId != null) {
                            playerSelections[category] = spellId
                        }
                    }
                    
                    if (playerSelections.isNotEmpty()) {
                        playerSelectedSpells[playerUUID] = playerSelections
                    }
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("Invalid UUID in spell selections file: $uuidString")
                }
            }
            
            plugin.logger.info("Loaded spell selections for ${playerSelectedSpells.size} players")
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to load spell selections", e)
        }
    }
    
    /**
     * Save spell selections to file
     */
    private fun saveSpellSelections() {
        try {
            // Clear existing data
            config = YamlConfiguration()
            
            // Save all player spell selections
            playerSelectedSpells.forEach { (playerUUID, selections) ->
                selections.forEach { (category, spellId) ->
                    config.set("$playerUUID.${category.name}", spellId)
                }
            }
            
            config.save(dataFile)
            
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Failed to save spell selections", e)
        }
    }
    
    /**
     * Save spell selections asynchronously to avoid blocking the main thread
     */
    private fun saveSpellSelectionsAsync() {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            saveSpellSelections()
        })
    }

    /**
     * Get the selected spell for a category
     */
    fun getSelectedSpell(player: Player, category: SpellCategory): Spell? {
        val playerUUID = player.uniqueId
        val selections = playerSelectedSpells[playerUUID] ?: return null
        val spellId = selections[category] ?: return null
        
        // Find the spell by ID
        return Spell.getAllSpells()[category]?.find { it.id == spellId }
    }
    
    /**
     * Set the selected spell for a category
     */
    fun setSelectedSpell(player: Player, category: SpellCategory, spell: Spell) {
        val playerUUID = player.uniqueId
        val selections = playerSelectedSpells.computeIfAbsent(playerUUID) { mutableMapOf() }
        selections[category] = spell.id
        
        // Save to file asynchronously
        saveSpellSelectionsAsync()
    }
    
    /**
     * Get all selected spells for a player
     */
    fun getAllSelectedSpells(player: Player): Map<SpellCategory, Spell> {
        val result = mutableMapOf<SpellCategory, Spell>()
        val playerUUID = player.uniqueId
        val selections = playerSelectedSpells[playerUUID] ?: return result
        
        SpellCategory.values().forEach { category ->
            val spellId = selections[category]
            if (spellId != null) {
                val spell = Spell.getAllSpells()[category]?.find { it.id == spellId }
                if (spell != null) {
                    result[category] = spell
                }
            }
        }
        
        return result
    }
    
    /**
     * Clear all selected spells for a player
     */
    fun clearSelectedSpells(player: Player) {
        playerSelectedSpells.remove(player.uniqueId)
        
        // Save to file asynchronously
        saveSpellSelectionsAsync()
    }
    
    /**
     * Deselect a spell for a specific category
     */
    fun deselectSpell(player: Player, category: SpellCategory) {
        val playerUUID = player.uniqueId
        val selections = playerSelectedSpells[playerUUID]
        
        if (selections != null) {
            selections.remove(category)
            
            // If no spells are selected anymore, remove the player entry entirely
            if (selections.isEmpty()) {
                playerSelectedSpells.remove(playerUUID)
            }
            
            // Save to file asynchronously
            saveSpellSelectionsAsync()
        }
    }
    
    /**
     * Check if player has a spell selected for a category
     */
    fun hasSelectedSpell(player: Player, category: SpellCategory): Boolean {
        return getSelectedSpell(player, category) != null
    }
    
    /**
     * Get available spells for a player based on their magic level
     */
    fun getAvailableSpells(player: Player, category: SpellCategory, magicLevel: Int): List<dev.cottage.cottageRPG.spells.Spell> {
        return Spell.getAllSpells()[category]?.filter { spell ->
            spell.requiredMagicLevel <= magicLevel
        } ?: emptyList()
    }
    
    /**
     * Get all spells a player can currently cast (has level and mana for)
     */
    fun getCastableSpells(player: Player, magicLevel: Int, playerMana: Double): Map<SpellCategory, List<dev.cottage.cottageRPG.spells.Spell>> {
        val result = mutableMapOf<SpellCategory, List<dev.cottage.cottageRPG.spells.Spell>>()
        
        SpellCategory.values().forEach { category ->
            val categorySpells = Spell.getAllSpells()[category]?.filter { spell ->
                spell.requiredMagicLevel <= magicLevel && spell.manaCost <= playerMana
            } ?: emptyList()
            
            if (categorySpells.isNotEmpty()) {
                result[category] = categorySpells
            }
        }
        
        return result
    }
    
    /**
     * Auto-select the best available spell for a category if none is selected
     */
    fun autoSelectSpell(player: Player, category: SpellCategory, magicLevel: Int): Boolean {
        if (hasSelectedSpell(player, category)) return false
        
        val availableSpells = getAvailableSpells(player, category, magicLevel)
        if (availableSpells.isEmpty()) return false
        
        // Select the spell with the highest level requirement (most advanced spell available)
        val bestSpell = availableSpells.maxByOrNull { it.requiredMagicLevel }
        if (bestSpell != null) {
            setSelectedSpell(player, category, bestSpell)
            return true
        }
        
        return false
    }
    
    /**
     * Get spell selection summary for a player
     */
    fun getSpellSummary(player: Player, magicLevel: Int, playerMana: Double): List<String> {
        val summary = mutableListOf<String>()
        summary.add("§5✦ Your Selected Spells ✦")
        summary.add("")
        
        SpellCategory.values().forEach { category ->
            val selectedSpell = getSelectedSpell(player, category)
            if (selectedSpell != null) {
                val canCast = selectedSpell.requiredMagicLevel <= magicLevel && selectedSpell.manaCost <= playerMana
                val statusIcon = if (canCast) "§a✓" else "§c✗"
                summary.add("§6${category.displayName}: $statusIcon §f${selectedSpell.name} §7(${selectedSpell.manaCost} mana)")
            } else {
                summary.add("§6${category.displayName}: §7None selected")
            }
        }
        
        summary.add("")
        summary.add("§7Use §6/spells§7 to change your selection")
        summary.add("§7Right-click with wand: Primary | Sneak+Right-click: Secondary | Sprint+Right-click: Special")
        
        return summary
    }
    
    /**
     * Validate and fix any invalid spell selections
     */
    fun validateSelections(player: Player, magicLevel: Int) {
        val playerUUID = player.uniqueId
        val selections = playerSelectedSpells[playerUUID] ?: return
        
        val toRemove = mutableListOf<SpellCategory>()
        
        selections.forEach { (category, spellId) ->
            val spell = Spell.getAllSpells()[category]?.find { it.id == spellId }
            if (spell == null || spell.requiredMagicLevel > magicLevel) {
                toRemove.add(category)
            }
        }
        
        if (toRemove.isNotEmpty()) {
            toRemove.forEach { category ->
                selections.remove(category)
                player.sendMessage("§c${category.displayName} spell removed - insufficient level!")
            }
            
            // Save changes to file
            saveSpellSelectionsAsync()
        }
    }
    
    /**
     * Force save all spell selections to file (useful for plugin shutdown)
     */
    fun forceSave() {
        saveSpellSelections()
    }
    
    /**
     * Reload spell selections from file
     */
    fun reload() {
        playerSelectedSpells.clear()
        loadSpellSelections()
    }
}
