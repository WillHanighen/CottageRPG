package dev.cottage.cottageRPG.gui

import dev.cottage.cottageRPG.CottageRPG
import dev.cottage.cottageRPG.spells.Spell
import dev.cottage.cottageRPG.spells.SpellCategory
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * GUI for spell selection with hierarchical category system
 */
class SpellSelectionGUI(private val plugin: CottageRPG) : Listener {

    // Map to track spell positions in category menus
    private val spellSlotMap = mutableMapOf<String, MutableMap<Int, Spell>>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    /**
     * Open the main spell category selection menu
     */
    fun open(player: Player) {
        val title = plugin.configManager.getString("gui.titles.spell_selection", "§5✦ Spell Selection ✦")
        val inventory = Bukkit.createInventory(null, 27, title)

        // Create border items
        val borderItem = createBorderItem()
        for (i in 0..8) inventory.setItem(i, borderItem)
        for (i in 18..26) inventory.setItem(i, borderItem)
        inventory.setItem(9, borderItem)
        inventory.setItem(17, borderItem)

        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val magicLevel = rpgPlayer.getSkillLevel("magic")

        // Primary spell category (slot 10)
        val primarySpell = plugin.playerSpellManager.getSelectedSpell(player, SpellCategory.PRIMARY)
        inventory.setItem(10, createCategoryItem(
            SpellCategory.PRIMARY,
            Material.FIRE_CHARGE,
            primarySpell,
            magicLevel
        ))

        // Secondary spell category (slot 12)
        val secondarySpell = plugin.playerSpellManager.getSelectedSpell(player, SpellCategory.SECONDARY)
        inventory.setItem(12, createCategoryItem(
            SpellCategory.SECONDARY,
            Material.SHIELD,
            secondarySpell,
            magicLevel
        ))

        // Special/Movement category (slot 14)
        val specialMovementSpell = plugin.playerSpellManager.getSelectedSpell(player, SpellCategory.SPECIAL_MOVEMENT)
        inventory.setItem(14, createCategoryItem(
            SpellCategory.SPECIAL_MOVEMENT,
            Material.ENDER_PEARL,
            specialMovementSpell,
            magicLevel
        ))

        // Info item (slot 16)
        inventory.setItem(16, createInfoItem(player, magicLevel))

        // Close button (slot 22)
        inventory.setItem(22, createCloseButton())

        player.openInventory(inventory)
    }

    /**
     * Open a specific spell category menu
     */
    fun openCategoryMenu(player: Player, category: SpellCategory) {
        val title = "§5✦ ${category.displayName} Spells ✦"
        val inventory = Bukkit.createInventory(null, 54, title)

        // Create border items
        val borderItem = createBorderItem()
        for (i in 0..8) inventory.setItem(i, borderItem)
        for (i in 45..53) inventory.setItem(i, borderItem)
        for (i in 9..44 step 9) inventory.setItem(i, borderItem)
        for (i in 17..44 step 9) inventory.setItem(i, borderItem)

        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val magicLevel = rpgPlayer.getSkillLevel("magic")
        val playerMana = rpgPlayer.mana

        // Get spells for this category
        val spells = Spell.getAllSpells()[category] ?: emptyList()
        val currentlySelected = plugin.playerSpellManager.getSelectedSpell(player, category)

        // Create unique key for this menu
        val menuKey = "${player.uniqueId}_${category.name}"
        spellSlotMap[menuKey] = mutableMapOf()

        // Place spells in the inventory
        var slot = 10
        spells.forEach { spell ->
            if (slot <= 43 && slot % 9 != 0 && slot % 9 != 8) {
                inventory.setItem(slot, createSpellItem(
                    spell,
                    magicLevel,
                    playerMana,
                    currentlySelected?.id == spell.id,
                    player
                ))
                // Store spell in slot map
                spellSlotMap[menuKey]!![slot] = spell
                slot++
                if (slot % 9 == 8) slot += 2 // Skip border slots
            }
        }

        // Back button (slot 48)
        inventory.setItem(48, createBackButton())

        // Close button (slot 50)
        inventory.setItem(50, createCloseButton())

        player.openInventory(inventory)
    }

    private fun createBorderItem(): ItemStack {
        val item = ItemStack(Material.PURPLE_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta?.let {
            it.setDisplayName("§r")
        }
        item.itemMeta = meta
        return item
    }

    private fun createCategoryItem(category: SpellCategory, defaultMaterial: Material,
                                   selectedSpell: Spell?, magicLevel: Int): ItemStack {
        val material = selectedSpell?.icon ?: defaultMaterial
        val item = ItemStack(material)
        val meta = item.itemMeta

        meta?.let {
            it.setDisplayName("${ChatColor.DARK_PURPLE}${ChatColor.BOLD}${category.displayName}")

            val lore = mutableListOf<String>()
            lore.add("${ChatColor.GRAY}${category.description}")
            lore.add("")

            if (selectedSpell != null) {
                lore.add("${ChatColor.GREEN}Currently Selected:")
                lore.add("${ChatColor.AQUA}${selectedSpell.name}")
                lore.add("${ChatColor.GRAY}${selectedSpell.description}")
                lore.add("${ChatColor.YELLOW}Mana Cost: ${ChatColor.WHITE}${selectedSpell.manaCost}")
                lore.add("${ChatColor.YELLOW}Cooldown: ${ChatColor.WHITE}${selectedSpell.cooldownSeconds}s")
            } else {
                lore.add("${ChatColor.RED}No spell selected")
            }

            lore.add("")
            lore.add("${ChatColor.YELLOW}Your Magic Level: ${ChatColor.WHITE}$magicLevel")
            lore.add("")
            lore.add("${ChatColor.YELLOW}Click to select spells!")

            it.lore = lore
        }
        item.itemMeta = meta
        return item
    }

    private fun createSpellItem(spell: Spell, playerMagicLevel: Int, playerMana: Double,
                                isSelected: Boolean, player: Player): ItemStack {
        val item = ItemStack(spell.icon)
        val meta = item.itemMeta

        val canUse = playerMagicLevel >= spell.requiredMagicLevel
        val hasEnoughMana = playerMana >= spell.manaCost
        val isOnCooldown = plugin.spellCooldownManager.isOnCooldown(player, spell.id)

        meta?.let {
            val nameColor = when {
                isSelected -> ChatColor.GREEN
                canUse && hasEnoughMana && !isOnCooldown -> ChatColor.AQUA
                else -> ChatColor.GRAY
            }

            val selectedIndicator = if (isSelected) " ${ChatColor.GREEN}✓" else ""
            it.setDisplayName("$nameColor${spell.name}$selectedIndicator")

            val lore = mutableListOf<String>()
            lore.add("${ChatColor.GRAY}${spell.description}")
            lore.add("${ChatColor.GRAY}${spell.effect}")
            lore.add("")
            lore.add("${ChatColor.YELLOW}Required Level: ${ChatColor.WHITE}${spell.requiredMagicLevel}")
            lore.add("${ChatColor.YELLOW}Mana Cost: ${ChatColor.WHITE}${spell.manaCost}")
            lore.add("${ChatColor.YELLOW}Cooldown: ${ChatColor.WHITE}${spell.cooldownSeconds}s")
            lore.add("")

            when {
                isSelected -> {
                    lore.add("${ChatColor.GREEN}✓ Currently Selected")
                    lore.add("${ChatColor.YELLOW}Click to deselect")
                }
                !canUse -> {
                    lore.add("${ChatColor.RED}✗ Insufficient Magic Level")
                    lore.add("${ChatColor.GRAY}Requires level ${spell.requiredMagicLevel}")
                }
                !hasEnoughMana -> {
                    lore.add("${ChatColor.RED}✗ Insufficient Mana")
                    lore.add("${ChatColor.GRAY}Need ${spell.manaCost} mana")
                }
                isOnCooldown -> {
                    val remaining = plugin.spellCooldownManager.getRemainingCooldown(player, spell.id)
                    lore.add("${ChatColor.RED}✗ On Cooldown")
                    lore.add("${ChatColor.GRAY}${remaining}s remaining")
                }
                else -> {
                    lore.add("${ChatColor.GREEN}✓ Available")
                    lore.add("${ChatColor.YELLOW}Click to select!")
                }
            }

            it.lore = lore
        }
        item.itemMeta = meta
        return item
    }

    private fun createInfoItem(player: Player, magicLevel: Int): ItemStack {
        val item = ItemStack(Material.BOOK)
        val meta = item.itemMeta
        val rpgPlayer = plugin.playerManager.getPlayer(player)

        meta?.let {
            it.setDisplayName("${ChatColor.AQUA}${ChatColor.BOLD}Spell Info")

            val lore = mutableListOf<String>()
            lore.add("${ChatColor.GRAY}Your current spell setup:")
            lore.add("")
            lore.add("${ChatColor.YELLOW}Magic Level: ${ChatColor.WHITE}$magicLevel")
            lore.add("${ChatColor.YELLOW}Current Mana: ${ChatColor.WHITE}${rpgPlayer.mana.toInt()}/${rpgPlayer.maxMana.toInt()}")
            lore.add("")
            lore.add("${ChatColor.GRAY}Wand Controls:")
            lore.add("${ChatColor.YELLOW}Right-click: ${ChatColor.WHITE}Cast Primary spell")
            lore.add("${ChatColor.YELLOW}Shift+Right-click: ${ChatColor.WHITE}Cast Secondary spell")
            lore.add("${ChatColor.YELLOW}Left-click: ${ChatColor.WHITE}Cast Special/Movement spell")
            lore.add("${ChatColor.YELLOW}Shift+Left-click: ${ChatColor.WHITE}Open this GUI")

            it.lore = lore
        }
        item.itemMeta = meta
        return item
    }

    private fun createBackButton(): ItemStack {
        val item = ItemStack(Material.ARROW)
        val meta = item.itemMeta
        meta?.let {
            it.setDisplayName("${ChatColor.GRAY}← Back")
            it.lore = listOf("${ChatColor.GRAY}Return to previous menu")
        }
        item.itemMeta = meta
        return item
    }

    private fun createCloseButton(): ItemStack {
        val item = ItemStack(Material.BARRIER)
        val meta = item.itemMeta
        meta?.let {
            it.setDisplayName("${ChatColor.RED}✕ Close")
            it.lore = listOf("${ChatColor.GRAY}Close spell selection")
        }
        item.itemMeta = meta
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = event.view.title

        // Only handle our GUI events
        if (!title.contains("§5✦") || !title.contains("✦")) return

        event.isCancelled = true

        // Handle different GUI types
        when {
            title == plugin.configManager.getString("gui.titles.spell_selection", "§5✦ Spell Selection ✦") -> {
                handleMainMenuClick(event, player)
            }
            title.contains("Spells ✦") -> {
                handleCategoryMenuClick(event, player)
            }
        }
    }

    private fun handleMainMenuClick(event: InventoryClickEvent, player: Player) {
        when (event.slot) {
            10 -> openCategoryMenu(player, SpellCategory.PRIMARY)
            12 -> openCategoryMenu(player, SpellCategory.SECONDARY)
            14 -> openCategoryMenu(player, SpellCategory.SPECIAL_MOVEMENT)
            22 -> player.closeInventory()
        }
    }

    private fun handleCategoryMenuClick(event: InventoryClickEvent, player: Player) {
        val clickedItem = event.currentItem ?: return

        when (event.slot) {
            48 -> {
                // Back button
                open(player)
            }
            50 -> player.closeInventory()
            else -> {
                // Handle spell selection using slot mapping
                handleSpellSelectionBySlot(event, player)
            }
        }
    }

    private fun handleSpellSelectionBySlot(event: InventoryClickEvent, player: Player) {
        val title = event.view.title
        val slot = event.slot

        // Determine category from title
        val category = when {
            title.contains("Primary") -> SpellCategory.PRIMARY
            title.contains("Secondary") -> SpellCategory.SECONDARY
            title.contains("Special") || title.contains("Movement") -> SpellCategory.SPECIAL_MOVEMENT
            else -> return
        }

        // Get spell from slot map
        val menuKey = "${player.uniqueId}_${category.name}"
        val spell = spellSlotMap[menuKey]?.get(slot) ?: return

        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val magicLevel = rpgPlayer.getSkillLevel("magic")
        val currentlySelected = plugin.playerSpellManager.getSelectedSpell(player, category)

        // Check if clicking on already selected spell (deselect)
        if (currentlySelected?.id == spell.id) {
            player.sendMessage("${ChatColor.YELLOW}${spell.name} ${ChatColor.GRAY}is already selected for ${category.displayName}!")
            return
        }

        // Check requirements
        val castResult = spell.canCast(player, magicLevel, rpgPlayer.mana)

        when (castResult) {
            dev.cottage.cottageRPG.spells.SpellCastResult.SUCCESS -> {
                plugin.playerSpellManager.setSelectedSpell(player, category, spell)
                player.sendMessage("${ChatColor.GREEN}✓ ${ChatColor.AQUA}${spell.name} ${ChatColor.GRAY}selected for ${ChatColor.YELLOW}${category.displayName}${ChatColor.GRAY}!")

                // Refresh the menu to show updated selection
                openCategoryMenu(player, category)
            }
            dev.cottage.cottageRPG.spells.SpellCastResult.INSUFFICIENT_LEVEL -> {
                player.sendMessage("${ChatColor.RED}You need Magic level ${spell.requiredMagicLevel} to select ${spell.name}!")
            }
            dev.cottage.cottageRPG.spells.SpellCastResult.INSUFFICIENT_MANA -> {
                player.sendMessage("${ChatColor.RED}You need ${spell.manaCost} mana to use ${spell.name}!")
            }
            else -> {
                player.sendMessage("${ChatColor.RED}Cannot select ${spell.name} right now!")
            }
        }
    }
}
