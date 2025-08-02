package dev.cottage.cottageRPG.gui

import dev.cottage.cottageRPG.CottageRPG
import dev.cottage.cottageRPG.classes.RPGClass
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * GUI for class selection
 */
class ClassSelectionGUI(private val plugin: CottageRPG) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }
    
    fun open(player: Player) {
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val title = plugin.configManager.getString("gui.titles.class_selection", "§6Choose Your Class")
        val inventory = Bukkit.createInventory(null, 27, title)
        
        val availableClasses = RPGClass.getDefaultClasses()
        val enabledClasses = plugin.configManager.getStringList("classes.available_classes")
        
        var slot = 10
        availableClasses.forEach { rpgClass ->
            if (enabledClasses.isEmpty() || enabledClasses.contains(rpgClass.id)) {
                val classItem = rpgClass.getIcon()
                val meta = classItem.itemMeta
                meta?.let {
                    val lore = it.lore?.toMutableList() ?: mutableListOf()
                    
                    // Add requirement status
                    lore.add("")
                    if (rpgClass.meetsRequirements(rpgPlayer.skills)) {
                        lore.add("§aRequirements: ✓ Met")
                        lore.add("§7Click to select this class!")
                    } else {
                        lore.add("§cRequirements: ✗ Not Met")
                        lore.add("§7Requirements:")
                        rpgClass.requirements.forEach { (skill, level) ->
                            val playerLevel = rpgPlayer.getSkillLevel(skill)
                            val color = if (playerLevel >= level) "§a" else "§c"
                            lore.add("§7- $skill: $color$playerLevel§7/$level")
                        }
                    }
                    
                    // Show current class status
                    if (rpgPlayer.rpgClass?.id == rpgClass.id) {
                        lore.add("")
                        lore.add("§6★ Current Class ★")
                    }
                    
                    it.lore = lore
                }
                classItem.itemMeta = meta
                inventory.setItem(slot, classItem)
                slot += 2
            }
        }
        
        // Current class info
        val currentClassItem = if (rpgPlayer.rpgClass != null) {
            val item = rpgPlayer.rpgClass!!.getIcon()
            val meta = item.itemMeta
            meta?.let {
                it.setDisplayName("§6Current Class: ${rpgPlayer.rpgClass!!.displayName}")
                val lore = mutableListOf<String>()
                lore.add("§7You are currently a ${rpgPlayer.rpgClass!!.displayName}")
                lore.add("")
                lore.add("§7Stats per level:")
                lore.add("§c❤ Health: +${rpgPlayer.rpgClass!!.healthPerLevel}")
                lore.add("§9✦ Mana: +${rpgPlayer.rpgClass!!.manaPerLevel}")
                
                if (plugin.configManager.getBoolean("classes.allow_class_change", true)) {
                    val cost = plugin.configManager.getInt("classes.class_change_cost", 10)
                    lore.add("")
                    lore.add("§7Class change cost: §e$cost levels")
                }
                
                it.lore = lore
            }
            item.itemMeta = meta
            item
        } else {
            val item = ItemStack(Material.BARRIER)
            val meta = item.itemMeta
            meta?.let {
                it.setDisplayName("§cNo Class Selected")
                it.lore = listOf(
                    "§7You haven't chosen a class yet!",
                    "§7Select one from the options above."
                )
            }
            item.itemMeta = meta
            item
        }
        inventory.setItem(4, currentClassItem)
        
        // Close button
        val closeItem = ItemStack(Material.BARRIER)
        val closeMeta = closeItem.itemMeta
        closeMeta?.let {
            it.setDisplayName("§cClose")
            it.lore = listOf("§7Click to close this menu")
        }
        closeItem.itemMeta = closeMeta
        inventory.setItem(26, closeItem)
        
        player.openInventory(inventory)
    }
    
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.inventory
        val title = plugin.configManager.getString("gui.titles.class_selection", "§6Choose Your Class")
        
        if (inventory.getHolder() == null && event.view.title == title) {
            event.isCancelled = true
            
            val clickedItem = event.currentItem ?: return
            val rpgPlayer = plugin.playerManager.getPlayer(player)
            val prefix = plugin.configManager.getString("general.prefix", "§7[§6CottageRPG§7]§r")
            
            when (event.slot) {
                26 -> {
                    // Close inventory
                    player.closeInventory()
                    return
                }
            }
            
            // Check if clicked item is a class
            val availableClasses = RPGClass.getDefaultClasses()
            val clickedClass = availableClasses.find { rpgClass ->
                clickedItem.type == rpgClass.icon && 
                clickedItem.itemMeta?.displayName?.contains(rpgClass.displayName) == true
            }
            
            if (clickedClass != null) {
                // Check if player already has this class
                if (rpgPlayer.rpgClass?.id == clickedClass.id) {
                    player.sendMessage("$prefix §cYou are already a ${clickedClass.displayName}!")
                    return
                }
                
                // Check requirements
                if (!clickedClass.meetsRequirements(rpgPlayer.skills)) {
                    player.sendMessage("$prefix §cYou don't meet the requirements for ${clickedClass.displayName}!")
                    return
                }
                
                // Check if changing class and if there's a cost
                if (rpgPlayer.rpgClass != null && plugin.configManager.getBoolean("classes.allow_class_change", true)) {
                    val cost = plugin.configManager.getInt("classes.class_change_cost", 10)
                    if (player.level < cost) {
                        player.sendMessage("$prefix §cYou need $cost experience levels to change your class!")
                        return
                    }
                    player.level -= cost
                    player.sendMessage("$prefix §7You paid $cost levels to change your class.")
                }
                
                rpgPlayer.setClass(clickedClass)
                player.closeInventory()
                
                // Show effects
                if (plugin.configManager.getBoolean("particles.enabled", true)) {
                    val particle = org.bukkit.Particle.valueOf(
                        plugin.configManager.getString("particles.level_up_particle", "FIREWORK")
                    )
                    player.world.spawnParticle(particle, player.location.add(0.0, 1.0, 0.0), 30)
                }
                
                if (plugin.configManager.getBoolean("sounds.enabled", true)) {
                    val sound = org.bukkit.Sound.valueOf(
                        plugin.configManager.getString("sounds.level_up_sound", "ENTITY_PLAYER_LEVELUP")
                    )
                    player.playSound(player.location, sound, 1.0f, 1.0f)
                }
            }
        }
    }
}
