package dev.cottage.cottageRPG.gui

import dev.cottage.cottageRPG.CottageRPG
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * GUI for displaying player stats and information
 */
class PlayerStatsGUI(private val plugin: CottageRPG) : Listener {
    
    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }
    
    fun open(player: Player) {
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val title = plugin.configManager.getString("gui.titles.player_stats", "§6Player Stats")
        val inventory = Bukkit.createInventory(null, 27, title)
        
        // Player info item
        val playerHead = ItemStack(Material.PLAYER_HEAD)
        val headMeta = playerHead.itemMeta
        headMeta?.let {
            it.setDisplayName("§6${player.name}")
            it.lore = listOf(
                "§7Level: §f${rpgPlayer.level}",
                "§7Experience: §f${rpgPlayer.experience}/${rpgPlayer.getExperienceForNextLevel()}",
                "§7Class: §f${rpgPlayer.rpgClass?.displayName ?: "None"}",
                "",
                "§7Health: §c${String.format("%.1f", rpgPlayer.health)}§7/§c${String.format("%.1f", rpgPlayer.maxHealth)}",
                "§7Mana: §9${String.format("%.1f", rpgPlayer.mana)}§7/§9${String.format("%.1f", rpgPlayer.maxMana)}",
                "§7Money: §e${plugin.configManager.getString("economy.currency_symbol", "$")}${String.format("%.2f", rpgPlayer.money)}"
            )
        }
        playerHead.itemMeta = headMeta
        inventory.setItem(4, playerHead)
        
        // Health item
        val healthItem = ItemStack(Material.RED_DYE)
        val healthMeta = healthItem.itemMeta
        healthMeta?.let {
            it.setDisplayName("§cHealth")
            it.lore = listOf(
                "§7Current: §c${String.format("%.1f", rpgPlayer.health)}",
                "§7Maximum: §c${String.format("%.1f", rpgPlayer.maxHealth)}",
                "§7Percentage: §c${String.format("%.1f", (rpgPlayer.health / rpgPlayer.maxHealth) * 100)}%"
            )
        }
        healthItem.itemMeta = healthMeta
        inventory.setItem(10, healthItem)
        
        // Mana item
        val manaItem = ItemStack(Material.BLUE_DYE)
        val manaMeta = manaItem.itemMeta
        manaMeta?.let {
            it.setDisplayName("§9Mana")
            it.lore = listOf(
                "§7Current: §9${String.format("%.1f", rpgPlayer.mana)}",
                "§7Maximum: §9${String.format("%.1f", rpgPlayer.maxMana)}",
                "§7Percentage: §9${String.format("%.1f", (rpgPlayer.mana / rpgPlayer.maxMana) * 100)}%"
            )
        }
        manaItem.itemMeta = manaMeta
        inventory.setItem(12, manaItem)
        
        // Experience item
        val expItem = ItemStack(Material.EXPERIENCE_BOTTLE)
        val expMeta = expItem.itemMeta
        expMeta?.let {
            it.setDisplayName("§aExperience")
            val expForNext = rpgPlayer.getExperienceForNextLevel()
            val progress = if (expForNext > 0) (rpgPlayer.experience.toDouble() / expForNext.toDouble()) * 100 else 100.0
            it.lore = listOf(
                "§7Level: §a${rpgPlayer.level}",
                "§7Current XP: §a${rpgPlayer.experience}",
                "§7XP for next level: §a$expForNext",
                "§7Progress: §a${String.format("%.1f", progress)}%"
            )
        }
        expItem.itemMeta = expMeta
        inventory.setItem(14, expItem)
        
        // Class item
        val classItem = if (rpgPlayer.rpgClass != null) {
            rpgPlayer.rpgClass!!.getIcon()
        } else {
            val item = ItemStack(Material.BARRIER)
            val meta = item.itemMeta
            meta?.let {
                it.setDisplayName("§cNo Class Selected")
                it.lore = listOf(
                    "§7You haven't chosen a class yet!",
                    "§7Use §e/class§7 to select one."
                )
            }
            item.itemMeta = meta
            item
        }
        inventory.setItem(16, classItem)
        
        // Skills overview
        val skillsItem = ItemStack(Material.ENCHANTED_BOOK)
        val skillsMeta = skillsItem.itemMeta
        skillsMeta?.let {
            it.setDisplayName("§6Skills Overview")
            val skillLore = mutableListOf<String>()
            skillLore.add("§7Click to view detailed skills")
            skillLore.add("")
            
            val topSkills = rpgPlayer.skills.entries.sortedByDescending { it.value }.take(5)
            if (topSkills.isNotEmpty()) {
                skillLore.add("§7Top Skills:")
                topSkills.forEach { (skill, level) ->
                    skillLore.add("§7- §e$skill§7: §f$level")
                }
            } else {
                skillLore.add("§7No skills developed yet")
            }
            
            it.lore = skillLore
        }
        skillsItem.itemMeta = skillsMeta
        inventory.setItem(22, skillsItem)
        
        // Navigation items
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
        val title = plugin.configManager.getString("gui.titles.player_stats", "§6Player Stats")
        
        if (inventory.getHolder() == null && event.view.title == title) {
            event.isCancelled = true
            
            when (event.slot) {
                22 -> {
                    // Open skills GUI
                    player.closeInventory()
                    SkillsGUI(plugin).open(player)
                }
                26 -> {
                    // Close inventory
                    player.closeInventory()
                }
            }
        }
    }
}
