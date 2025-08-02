package dev.cottage.cottageRPG.gui

import dev.cottage.cottageRPG.CottageRPG
import dev.cottage.cottageRPG.player.RPGPlayer
import dev.cottage.cottageRPG.skills.Skill
import dev.cottage.cottageRPG.skills.SkillCategory
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * GUI for displaying player skills using PaperMC APIs (with legacy ChatColor for compatibility)
 * Organized with borders and category sections
 */
class SkillsGUI(private val plugin: CottageRPG) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun open(player: Player) {
        val rpgPlayer = plugin.playerManager.getPlayer(player) as RPGPlayer
        val inventory = Bukkit.createInventory(null, 54, "Skills")

        // Get all skills organized by category
        val skills = Skill.getDefaultSkills()
        val gatheringSkills = skills.filter { it.category == SkillCategory.GATHERING }
        val combatSkills = skills.filter { it.category == SkillCategory.COMBAT }
        val craftingSkills = skills.filter { it.category == SkillCategory.CRAFTING }
        val magicSkills = skills.filter { it.category == SkillCategory.MAGIC }

        // Create border items
        val borderItem = createBorderItem()

        // Fill all borders
        // Top border (row 0: slots 0-8)
        for (i in 0..8) inventory.setItem(i, borderItem)

        // Bottom border (row 5: slots 45-53)
        for (i in 45..53) inventory.setItem(i, borderItem)

        // Side borders for rows 1-4
        for (row in 1..4) {
            inventory.setItem(row * 9, borderItem)      // Left border
            inventory.setItem(row * 9 + 8, borderItem)  // Right border
        }

        // Row 1 (slots 9-17): Gathering section
        inventory.setItem(10, createCategoryHeader(SkillCategory.GATHERING, Material.WOODEN_PICKAXE))
        placeSkillsInSection(inventory, gatheringSkills, rpgPlayer, 12, 6)

        // Row 2 (slots 18-26): Combat section
        inventory.setItem(19, createCategoryHeader(SkillCategory.COMBAT, Material.IRON_SWORD))
        placeSkillsInSection(inventory, combatSkills, rpgPlayer, 21, 6)

        // Row 3 (slots 27-35): Crafting section
        inventory.setItem(28, createCategoryHeader(SkillCategory.CRAFTING, Material.CRAFTING_TABLE))
        placeSkillsInSection(inventory, craftingSkills, rpgPlayer, 30, 6)

        // Row 4 (slots 36-44): Magic section
        inventory.setItem(37, createCategoryHeader(SkillCategory.MAGIC, Material.ENCHANTED_BOOK))
        placeSkillsInSection(inventory, magicSkills, rpgPlayer, 39, 6)

        // Navigation in bottom border
        inventory.setItem(46, createBackButton())
        inventory.setItem(49, createSkillSummary(rpgPlayer, gatheringSkills, combatSkills, craftingSkills, magicSkills))
        inventory.setItem(52, createCloseButton())

        player.openInventory(inventory)
    }

    private fun placeSkillsInSection(inventory: Inventory, skills: List<Skill>, rpgPlayer: RPGPlayer,
                                     startSlot: Int, maxSkills: Int) {
        val fillerItem = createFillerItem()

        // Place skills up to the maximum allowed
        skills.take(maxSkills).forEachIndexed { index, skill ->
            val slot = startSlot + index
            val skillLevel = rpgPlayer.getSkillLevel(skill.id)
            val skillExp = rpgPlayer.skillExperience[skill.id] ?: 0L
            val expForNext = rpgPlayer.getSkillExperienceForNextLevel(skillLevel)
            val totalExp = rpgPlayer.getTotalSkillExperience(skill.id)
            val skillIcon = skill.getDetailedIcon(skillLevel, skillExp, expForNext, totalExp)
            inventory.setItem(slot, skillIcon)
        }

        // Fill remaining slots in section with filler
        for (i in (startSlot + skills.size) until (startSlot + maxSkills)) {
            inventory.setItem(i, fillerItem)
        }
    }

    private fun createBorderItem(): ItemStack {
        val item = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta?.setDisplayName(" ")
        item.itemMeta = meta
        return item
    }

    private fun createFillerItem(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta?.setDisplayName(" ")
        item.itemMeta = meta
        return item
    }

    private fun createCategoryHeader(category: SkillCategory, material: Material): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta?.let {
            val color = when (category) {
                SkillCategory.GATHERING -> ChatColor.DARK_GREEN
                SkillCategory.COMBAT -> ChatColor.RED
                SkillCategory.CRAFTING -> ChatColor.GOLD
                SkillCategory.MAGIC -> ChatColor.DARK_PURPLE
            }

            val description = when (category) {
                SkillCategory.GATHERING -> "Resource collection skills"
                SkillCategory.COMBAT -> "Battle and warfare skills"
                SkillCategory.CRAFTING -> "Item creation skills"
                SkillCategory.MAGIC -> "Mystical abilities"
            }

            it.setDisplayName("$color━━ ${category.displayName} ━━")
            it.lore = listOf(
                "${ChatColor.GRAY}$description",
                "",
                "${ChatColor.YELLOW}Skills in this category →"
            )
        }
        item.itemMeta = meta
        return item
    }

    private fun createBackButton(): ItemStack {
        val item = ItemStack(Material.ARROW)
        val meta = item.itemMeta
        meta?.let {
            it.setDisplayName("${ChatColor.GRAY}← Back")
            it.lore = listOf("${ChatColor.GRAY}Return to stats")
        }
        item.itemMeta = meta
        return item
    }

    private fun createCloseButton(): ItemStack {
        val item = ItemStack(Material.BARRIER)
        val meta = item.itemMeta
        meta?.let {
            it.setDisplayName("${ChatColor.RED}✕ Close")
            it.lore = listOf("${ChatColor.GRAY}Close menu")
        }
        item.itemMeta = meta
        return item
    }

    private fun createSkillSummary(rpgPlayer: RPGPlayer, gatheringSkills: List<Skill>, combatSkills: List<Skill>,
                                   craftingSkills: List<Skill>, magicSkills: List<Skill>): ItemStack {
        val item = ItemStack(Material.KNOWLEDGE_BOOK)
        val meta = item.itemMeta
        meta?.let {
            it.setDisplayName("${ChatColor.AQUA}📊 Overview")

            val totalLevels = rpgPlayer.skills.values.sum()
            val skillCount = rpgPlayer.skills.size
            val averageLevel = if (skillCount > 0) totalLevels.toDouble() / skillCount else 0.0

            val lore = mutableListOf<String>()
            lore.add("${ChatColor.GRAY}━━━━━━━━━━━━━━━━━━━━")
            lore.add("${ChatColor.YELLOW}Total Levels: ${ChatColor.WHITE}$totalLevels")
            lore.add("${ChatColor.YELLOW}Skills: ${ChatColor.WHITE}$skillCount")
            lore.add("${ChatColor.YELLOW}Average: ${ChatColor.WHITE}${String.format("%.1f", averageLevel)}")
            lore.add("")
            lore.add("${ChatColor.DARK_GREEN}Gathering: ${ChatColor.WHITE}${gatheringSkills.size}")
            lore.add("${ChatColor.RED}Combat: ${ChatColor.WHITE}${combatSkills.size}")
            lore.add("${ChatColor.GOLD}Crafting: ${ChatColor.WHITE}${craftingSkills.size}")
            lore.add("${ChatColor.DARK_PURPLE}Magic: ${ChatColor.WHITE}${magicSkills.size}")
            lore.add("${ChatColor.GRAY}━━━━━━━━━━━━━━━━━━━━")

            it.lore = lore
        }
        item.itemMeta = meta
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.view.title != "Skills") return
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        val item = event.currentItem ?: return

        when (event.slot) {
            46 -> {
                // Back to stats
                player.closeInventory()
                PlayerStatsGUI(plugin).open(player)
            }
            52 -> {
                // Close menu
                player.closeInventory()
            }
            // Category headers
            10, 19, 28, 37 -> {
                player.sendMessage("${ChatColor.GRAY}Click on individual skills for details!")
            }
            // Skill slots - let skills handle their own events
            in 11..16, in 20..25, in 29..34, in 38..43 -> {
                // Skills handle their own click events
            }
        }
    }
}
