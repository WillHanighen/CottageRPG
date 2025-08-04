package dev.cottage.cottageRPG.commands

import dev.cottage.cottageRPG.CottageRPG
import dev.cottage.cottageRPG.gui.PlayerStatsGUI
import dev.cottage.cottageRPG.gui.ClassSelectionGUI
import dev.cottage.cottageRPG.gui.SkillsGUI
import dev.cottage.cottageRPG.gui.SpellSelectionGUI
import dev.cottage.cottageRPG.skills.Skill
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Main command handler for RPG commands
 * Handles all RPG-related commands with comprehensive functionality and tab completion
 */
class RPGCommand(private val plugin: CottageRPG) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cThis command can only be used by players!")
            return true
        }
        
        // Check if player has basic permission
        if (!sender.hasPermission("cottageRPG.use")) {
            sender.sendMessage("§cYou don't have permission to use CottageRPG commands!")
            return true
        }
        
        val prefix = plugin.configManager.getString("general.prefix", "§7[§6CRPG§7]§r")
        
        try {
            when (command.name.lowercase()) {
                "rpg" -> {
                    if (args.isEmpty()) {
                        showHelp(sender)
                        return true
                    }
                    
                    when (args[0].lowercase()) {
                        "stats" -> {
                            PlayerStatsGUI(plugin).open(sender)
                        }
                        "skills" -> {
                            SkillsGUI(plugin).open(sender)
                        }
                        "class" -> {
                            if (args.size > 1) {
                                handleClassCommand(sender, args[1])
                            } else {
                                ClassSelectionGUI(plugin).open(sender)
                            }
                        }
                        "reload" -> {
                            if (sender.hasPermission("cottageRPG.admin.reload")) {
                                plugin.configManager.reloadConfig()
                                plugin.playerManager.reload()
                                sender.sendMessage("$prefix §aConfiguration reloaded!")
                            } else {
                                sender.sendMessage("$prefix §cYou don't have permission to use this command!")
                            }
                        }
                        "admin" -> {
                            if (args.size < 2) {
                                showAdminHelp(sender)
                                return true
                            }
                            handleAdminCommand(sender, args.drop(1).toTypedArray())
                        }
                        "top" -> {
                            showTopPlayers(sender)
                        }
                        "help" -> {
                            showHelp(sender)
                        }
                        "scoreboard", "sb" -> {
                            toggleScoreboard(sender)
                        }
                        "spells" -> {
                            if (args.size > 1) {
                                handleSpellCommand(sender, args.drop(1).toTypedArray())
                            } else {
                                SpellSelectionGUI(plugin).open(sender)
                            }
                        }
                        else -> {
                            sender.sendMessage("$prefix §cUnknown subcommand: §e${args[0]}")
                            sender.sendMessage("§7Use §e/rpg help §7for available commands.")
                        }
                    }
                }
                "stats" -> {
                    PlayerStatsGUI(plugin).open(sender)
                }
                "skills" -> {
                    SkillsGUI(plugin).open(sender)
                }
                "class" -> {
                    if (args.isNotEmpty()) {
                        handleClassCommand(sender, args[0])
                    } else {
                        ClassSelectionGUI(plugin).open(sender)
                    }
                }
                "scoreboard", "sb", "rpgboard" -> {
                    toggleScoreboard(sender)
                }
                "spells" -> {
                    if (args.isNotEmpty()) {
                        handleSpellCommand(sender, args)
                    } else {
                        SpellSelectionGUI(plugin).open(sender)
                    }
                }
            }
        } catch (e: Exception) {
            sender.sendMessage("$prefix §cAn error occurred while executing the command. Please try again.")
            plugin.logger.warning("Error executing command '${command.name}' for player ${sender.name}: ${e.message}")
            e.printStackTrace()
        }
        
        return true
    }
    
    private fun showHelp(player: Player) {
        val prefix = plugin.configManager.getString("general.prefix", "§7[§6CottageRPG§7]§r")
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        
        player.sendMessage("$prefix §6=== CottageRPG Commands ===")
        player.sendMessage("§e/rpg stats §7- View your character stats (Level ${rpgPlayer.level})")
        player.sendMessage("§e/rpg skills §7- View your skills and experience")
        player.sendMessage("§e/rpg class §7- Choose or view your class${if (rpgPlayer.rpgClass != null) " (Current: ${rpgPlayer.rpgClass!!.displayName})" else ""}")
        player.sendMessage("§e/rpg top §7- View top players leaderboard")
        player.sendMessage("")
        player.sendMessage("§6Quick Access Commands:")
        player.sendMessage("§e/stats §7- Quick access to stats GUI")
        player.sendMessage("§e/skills §7- Quick access to skills GUI")
        player.sendMessage("§e/class [name] §7- Quick class selection")
        player.sendMessage("§e/scoreboard §7- Toggle scoreboard")
        player.sendMessage("§e/spells §7- View and manage your spells")
        
        if (player.hasPermission("cottageRPG.admin.reload")) {
            player.sendMessage("")
            player.sendMessage("§cAdmin Commands:")
            player.sendMessage("§e/rpg reload §7- Reload plugin configuration")
        }
        
        player.sendMessage("")
        player.sendMessage("§7Use §e/rpg <command> §7for more information!")
    }
    
    private fun handleClassCommand(player: Player, className: String) {
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val prefix = plugin.configManager.getString("general.prefix", "§7[§6CottageRPG§7]§r")
        
        if (!plugin.configManager.getBoolean("classes.enabled", true)) {
            player.sendMessage("$prefix §cClass system is currently disabled!")
            return
        }
        
        val availableClasses = dev.cottage.cottageRPG.classes.RPGClass.getDefaultClasses()
        
        // Special case: if className is "list" or "all", show all available classes
        if (className.equals("list", ignoreCase = true) || className.equals("all", ignoreCase = true)) {
            showAvailableClasses(player, availableClasses, rpgPlayer)
            return
        }
        
        val targetClass = availableClasses.find { it.id.equals(className, ignoreCase = true) }
        
        if (targetClass == null) {
            player.sendMessage("$prefix §cClass '$className' not found!")
            player.sendMessage("§7Available classes: ${availableClasses.joinToString("§7, §e") { it.id }}")
            player.sendMessage("§7Use §e/class list §7to see detailed information about all classes.")
            return
        }
        
        // Check if player already has this class
        if (rpgPlayer.rpgClass?.id == targetClass.id) {
            player.sendMessage("$prefix §aYou are already a ${targetClass.displayName}!")
            showClassDetails(player, targetClass, rpgPlayer)
            return
        }
        
        // Check requirements
        if (!targetClass.meetsRequirements(rpgPlayer.skills)) {
            player.sendMessage("$prefix §cYou don't meet the requirements for ${targetClass.displayName}!")
            player.sendMessage("§7Requirements:")
            targetClass.requirements.forEach { (skill, level) ->
                val playerLevel = rpgPlayer.getSkillLevel(skill)
                val color = if (playerLevel >= level) "§a" else "§c"
                player.sendMessage("§7- $skill: $color$playerLevel§7/$level")
            }
            return
        }
        
        // Check if changing class and if there's a cost
        if (rpgPlayer.rpgClass != null && plugin.configManager.getBoolean("classes.allow_class_change", true)) {
            val cost = plugin.configManager.getInt("classes.class_change_cost", 10)
            if (player.level < cost) {
                player.sendMessage("$prefix §cYou need $cost experience levels to change your class!")
                player.sendMessage("§7Current level: §e${player.level}§7, Required: §e$cost")
                return
            }
            player.sendMessage("$prefix §7Changing class will cost §e$cost §7experience levels.")
            player.level -= cost
        }
        
        rpgPlayer.setClass(targetClass)
        player.sendMessage("$prefix §aYou are now a ${targetClass.displayName}!")
        showClassDetails(player, targetClass, rpgPlayer)
    }
    
    private fun showAvailableClasses(player: Player, classes: List<dev.cottage.cottageRPG.classes.RPGClass>, rpgPlayer: dev.cottage.cottageRPG.player.RPGPlayer) {
        val prefix = plugin.configManager.getString("general.prefix", "§7[§6CottageRPG§7]§r")
        player.sendMessage("$prefix §6=== Available Classes ===")
        
        classes.forEach { rpgClass ->
            val meetsReqs = rpgClass.meetsRequirements(rpgPlayer.skills)
            val statusColor = if (meetsReqs) "§a" else "§c"
            val status = if (meetsReqs) "✓" else "✗"
            val current = if (rpgPlayer.rpgClass?.id == rpgClass.id) " §b(Current)" else ""
            
            player.sendMessage("$statusColor$status §e${rpgClass.displayName}$current §7- ${rpgClass.description}")
            
            if (!meetsReqs) {
                player.sendMessage("  §7Requirements: ${rpgClass.requirements.entries.joinToString(", ") { "${it.key} ${it.value}" }}")
            }
        }
        
        player.sendMessage("")
        player.sendMessage("§7Use §e/class <name> §7to select a class.")
    }
    
    private fun showClassDetails(player: Player, rpgClass: dev.cottage.cottageRPG.classes.RPGClass, rpgPlayer: dev.cottage.cottageRPG.player.RPGPlayer) {
        player.sendMessage("§6=== ${rpgClass.displayName} ===")
        player.sendMessage("§7${rpgClass.description}")
        
        if (rpgClass.requirements.isNotEmpty()) {
            player.sendMessage("§7Requirements:")
            rpgClass.requirements.forEach { (skill, level) ->
                val playerLevel = rpgPlayer.getSkillLevel(skill)
                val color = if (playerLevel >= level) "§a" else "§c"
                player.sendMessage("§7- $skill: $color$playerLevel§7/$level")
            }
        }
        
        // Show class bonuses if available
        player.sendMessage("§7Class bonuses and abilities will be displayed here in future updates!")
    }
    
    private fun showTopPlayers(player: Player) {
        val prefix = plugin.configManager.getString("general.prefix", "§7[§6CottageRPG§7]§r")
        val topPlayers = plugin.playerManager.databaseManager.getTopPlayersByLevel(10)
        
        player.sendMessage("$prefix §6=== Top Players ===")
        
        if (topPlayers.isEmpty()) {
            player.sendMessage("§7No player data available yet!")
            player.sendMessage("§7Start playing to see rankings!")
            return
        }
        
        topPlayers.forEachIndexed { index, (name, level) ->
            val rank = index + 1
            val medal = when (rank) {
                1 -> "§6🥇"
                2 -> "§7🥈"
                3 -> "§c🥉"
                else -> "§e$rank."
            }
            val highlight = if (name == player.name) "§b" else "§f"
            player.sendMessage("$medal $highlight$name §7- Level §6$level")
        }
        
        // Show player's own rank if not in top 10
        val playerRank = plugin.playerManager.databaseManager.getPlayerRank(player.uniqueId)
        if (playerRank > 10 && playerRank != -1) {
            player.sendMessage("")
            player.sendMessage("§7Your rank: §e#$playerRank")
        }
    }
    
    private fun toggleScoreboard(player: Player) {
        val prefix = plugin.configManager.getString("general.prefix", "§7[§6CottageRPG§7]§r")
        val scoreboardManager = plugin.scoreboardManager
        
        if (scoreboardManager.isScoreboardEnabled(player)) {
            scoreboardManager.disableScoreboard(player)
            player.sendMessage("$prefix §cScoreboard disabled.")
        } else {
            scoreboardManager.enableScoreboard(player)
            player.sendMessage("$prefix §aScoreboard enabled.")
        }
    }
    
    private fun showAdminHelp(player: Player) {
        val prefix = plugin.configManager.getString("general.prefix", "§7[§6CottageRPG§7]§r")
        
        if (!player.hasPermission("cottageRPG.admin")) {
            player.sendMessage("$prefix §cYou don't have permission to use admin commands!")
            return
        }
        
        player.sendMessage("$prefix §6=== Admin Commands ===")
        player.sendMessage("§e/rpg admin setlevel <player> <level> §7- Set a player's level")
        player.sendMessage("§e/rpg admin setskill <player> <skill> <level> §7- Set a player's skill level")
        player.sendMessage("§e/rpg admin setexp <player> <amount> §7- Set a player's experience")
        player.sendMessage("§e/rpg admin setmoney <player> <amount> §7- Set a player's money")
        player.sendMessage("§e/rpg admin setclass <player> <class> §7- Set a player's class")
        player.sendMessage("§e/rpg admin resetcooldowns <player> §7- Reset a player's spell cooldowns")
        player.sendMessage("§e/rpg admin addskillexp <player> <skill> <amount> §7- Add skill experience")
        player.sendMessage("§e/rpg admin addexp <player> <amount> §7- Add experience")
    }
    
    private fun handleAdminCommand(player: Player, args: Array<String>) {
        val prefix = plugin.configManager.getString("general.prefix", "§7[§6CottageRPG§7]§r")
        
        if (!player.hasPermission("cottageRPG.admin")) {
            player.sendMessage("$prefix §cYou don't have permission to use admin commands!")
            return
        }
        
        if (args.size < 2) {
            showAdminHelp(player)
            return
        }
        
        val subcommand = args[0].lowercase()
        val targetPlayerName = args[1]
        val targetPlayer = plugin.server.getPlayer(targetPlayerName)
        
        if (targetPlayer == null) {
            player.sendMessage("$prefix §cPlayer '$targetPlayerName' not found!")
            return
        }
        
        val rpgPlayer = plugin.playerManager.getPlayer(targetPlayer)
        
        when (subcommand) {
            "setlevel" -> {
                if (args.size < 3) {
                    player.sendMessage("$prefix §cUsage: /rpg admin setlevel <player> <level>")
                    return
                }
                val level = args[2].toIntOrNull()
                if (level == null || level < 1) {
                    player.sendMessage("$prefix §cInvalid level! Must be a positive number.")
                    return
                }
                rpgPlayer.level = level
                player.sendMessage("$prefix §aSet ${targetPlayer.name}'s level to $level!")
                targetPlayer.sendMessage("$prefix §aYour level has been set to $level by an admin!")
            }
            "setskill" -> {
                if (args.size < 4) {
                    player.sendMessage("$prefix §cUsage: /rpg admin setskill <player> <skill> <level>")
                    return
                }
                val skill = args[2]
                val level = args[3].toIntOrNull()
                if (level == null || level < 0 || level > 100) {
                    player.sendMessage("$prefix §cInvalid level! Must be between 0 and 100.")
                    return
                }
                rpgPlayer.setSkillLevel(skill, level)
                player.sendMessage("$prefix §aSet ${targetPlayer.name}'s $skill level to $level!")
                targetPlayer.sendMessage("$prefix §aYour $skill level has been set to $level by an admin!")
            }
            "setexp" -> {
                if (args.size < 3) {
                    player.sendMessage("$prefix §cUsage: /rpg admin setexp <player> <amount>")
                    return
                }
                val amount = args[2].toLongOrNull()
                if (amount == null || amount < 0) {
                    player.sendMessage("$prefix §cInvalid amount! Must be a non-negative number.")
                    return
                }
                rpgPlayer.experience = amount
                player.sendMessage("$prefix §aSet ${targetPlayer.name}'s experience to $amount!")
                targetPlayer.sendMessage("$prefix §aYour experience has been set to $amount by an admin!")
            }
            "addexp" -> {
                if (args.size < 3) {
                    player.sendMessage("$prefix §cUsage: /rpg admin addexp <player> <amount>")
                    return
                }
                val amount = args[2].toLongOrNull()
                if (amount == null) {
                    player.sendMessage("$prefix §cInvalid amount! Must be a number.")
                    return
                }
                val leveledUp = rpgPlayer.addExperience(amount)
                player.sendMessage("$prefix §aAdded $amount experience to ${targetPlayer.name}!")
                if (leveledUp) {
                    player.sendMessage("$prefix §e${targetPlayer.name} leveled up!")
                }
                targetPlayer.sendMessage("$prefix §aYou gained $amount experience from an admin!")
            }
            "setmoney" -> {
                if (args.size < 3) {
                    player.sendMessage("$prefix §cUsage: /rpg admin setmoney <player> <amount>")
                    return
                }
                val amount = args[2].toDoubleOrNull()
                if (amount == null || amount < 0) {
                    player.sendMessage("$prefix §cInvalid amount! Must be a non-negative number.")
                    return
                }
                rpgPlayer.money = amount
                player.sendMessage("$prefix §aSet ${targetPlayer.name}'s money to $amount!")
                targetPlayer.sendMessage("$prefix §aYour money has been set to $amount by an admin!")
            }
            "setclass" -> {
                if (args.size < 3) {
                    player.sendMessage("$prefix §cUsage: /rpg admin setclass <player> <class>")
                    return
                }
                val className = args[2]
                val targetClass = dev.cottage.cottageRPG.classes.RPGClass.getDefaultClasses().find { it.id.equals(className, ignoreCase = true) }
                if (targetClass == null) {
                    player.sendMessage("$prefix §cClass '$className' not found!")
                    val availableClasses = dev.cottage.cottageRPG.classes.RPGClass.getDefaultClasses().joinToString(", ") { it.id }
                    player.sendMessage("§7Available classes: $availableClasses")
                    return
                }
                rpgPlayer.setClass(targetClass)
                player.sendMessage("$prefix §aSet ${targetPlayer.name}'s class to ${targetClass.displayName}!")
                targetPlayer.sendMessage("$prefix §aYour class has been set to ${targetClass.displayName} by an admin!")
            }
            "addskillexp" -> {
                if (args.size < 4) {
                    player.sendMessage("$prefix §cUsage: /rpg admin addskillexp <player> <skill> <amount>")
                    return
                }
                val skill = args[2]
                val amount = args[3].toLongOrNull()
                if (amount == null) {
                    player.sendMessage("$prefix §cInvalid amount! Must be a number.")
                    return
                }
                val leveledUp = rpgPlayer.addSkillExperience(skill, amount)
                player.sendMessage("$prefix §aAdded $amount $skill experience to ${targetPlayer.name}!")
                if (leveledUp) {
                    player.sendMessage("$prefix §e${targetPlayer.name}'s $skill skill leveled up!")
                }
                targetPlayer.sendMessage("$prefix §aYou gained $amount $skill experience from an admin!")
            }
            "resetcooldowns" -> {
                // Check if spell cooldown manager exists
                if (plugin.server.pluginManager.getPlugin("CottageRPG")?.javaClass?.getDeclaredField("spellCooldownManager") != null) {
                    try {
                        val field = plugin.javaClass.getDeclaredField("spellCooldownManager")
                        field.isAccessible = true
                        val cooldownManager = field.get(plugin)
                        val clearMethod = cooldownManager.javaClass.getMethod("clearAllCooldowns", org.bukkit.entity.Player::class.java)
                        clearMethod.invoke(cooldownManager, targetPlayer)
                        player.sendMessage("$prefix §aReset ${targetPlayer.name}'s spell cooldowns!")
                        targetPlayer.sendMessage("$prefix §aYour spell cooldowns have been reset by an admin!")
                    } catch (e: Exception) {
                        player.sendMessage("$prefix §cCould not reset spell cooldowns: ${e.message}")
                    }
                } else {
                    player.sendMessage("$prefix §cSpell cooldown system not found!")
                }
            }
            else -> {
                player.sendMessage("$prefix §cUnknown admin command: $subcommand")
                showAdminHelp(player)
            }
        }
    }

    private fun handleSpellCommand(player: Player, args: Array<out String>) {
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val magicLevel = rpgPlayer.getSkillLevel("magic")
        val playerMana = rpgPlayer.mana
        val prefix = plugin.configManager.getString("general.prefix", "§7[§6CRPG§7]§r")

        when (args[0].lowercase()) {
            "info", "i" -> {
                // Show detailed spell information
                val summary = plugin.playerSpellManager.getSpellSummary(player, magicLevel, playerMana)
                summary.forEach { player.sendMessage(it) }
            }
            "list", "l" -> {
                // Show all available spells by category
                if (args.size > 1) {
                    val categoryName = args[1].lowercase()
                    val category = when (categoryName) {
                        "primary", "p" -> dev.cottage.cottageRPG.spells.SpellCategory.PRIMARY
                        "secondary", "s" -> dev.cottage.cottageRPG.spells.SpellCategory.SECONDARY
                        "special", "movement", "sm" -> dev.cottage.cottageRPG.spells.SpellCategory.SPECIAL_MOVEMENT
                        else -> {
                            player.sendMessage("$prefix §cInvalid category! Use: primary, secondary, or special")
                            return
                        }
                    }
                    showSpellsByCategory(player, category, magicLevel, playerMana)
                } else {
                    showAllSpells(player, magicLevel, playerMana)
                }
            }
            "cooldowns", "cd" -> {
                // Show active cooldowns
                val cooldownDisplay = plugin.spellCooldownManager.getCooldownDisplay(player)
                cooldownDisplay.forEach { player.sendMessage(it) }
            }
            "select", "set" -> {
                // Quick spell selection
                if (args.size < 3) {
                    player.sendMessage("$prefix §cUsage: /spells select <category> <spell_name>")
                    player.sendMessage("§7Categories: primary, secondary, special")
                    return
                }

                val categoryName = args[1].lowercase()
                val spellName = args.drop(2).joinToString(" ").lowercase()

                val category = when (categoryName) {
                    "primary", "p" -> dev.cottage.cottageRPG.spells.SpellCategory.PRIMARY
                    "secondary", "s" -> dev.cottage.cottageRPG.spells.SpellCategory.SECONDARY
                    "special", "movement", "sm" -> dev.cottage.cottageRPG.spells.SpellCategory.SPECIAL_MOVEMENT
                    else -> {
                        player.sendMessage("$prefix §cInvalid category! Use: primary, secondary, or special")
                        return
                    }
                }

                val availableSpells = plugin.playerSpellManager.getAvailableSpells(player, category, magicLevel)
                val spell = availableSpells.find { it.name.lowercase().contains(spellName) }

                if (spell == null) {
                    player.sendMessage("$prefix §cSpell not found or not available!")
                    return
                }

                plugin.playerSpellManager.setSelectedSpell(player, category, spell)
                player.sendMessage("$prefix §aSelected §d${spell.name} §afor ${category.displayName} category!")
            }
            "clear" -> {
                // Clear spell selections
                if (args.size > 1) {
                    val categoryName = args[1].lowercase()
                    val category = when (categoryName) {
                        "primary", "p" -> dev.cottage.cottageRPG.spells.SpellCategory.PRIMARY
                        "secondary", "s" -> dev.cottage.cottageRPG.spells.SpellCategory.SECONDARY
                        "special", "movement", "sm" -> dev.cottage.cottageRPG.spells.SpellCategory.SPECIAL_MOVEMENT
                        "all" -> {
                            plugin.playerSpellManager.clearSelectedSpells(player)
                            player.sendMessage("$prefix §aCleared all spell selections!")
                            return
                        }
                        else -> {
                            player.sendMessage("$prefix §cInvalid category! Use: primary, secondary, special, or all")
                            return
                        }
                    }

                    plugin.playerSpellManager.deselectSpell(player, category)
                    player.sendMessage("$prefix §aCleared ${category.displayName} spell selection!")
                } else {
                    plugin.playerSpellManager.clearSelectedSpells(player)
                    player.sendMessage("$prefix §aCleared all spell selections!")
                }
            }
            "help", "h" -> {
                showSpellHelp(player)
            }
            else -> {
                player.sendMessage("$prefix §cUnknown spell command: §e${args[0]}")
                showSpellHelp(player)
            }
        }
    }
    
    private fun showSpellsByCategory(player: Player, category: dev.cottage.cottageRPG.spells.SpellCategory, magicLevel: Int, playerMana: Double) {
        val spells = dev.cottage.cottageRPG.spells.Spell.getAllSpells()[category] ?: emptyList()
        val selectedSpell = plugin.playerSpellManager.getSelectedSpell(player, category)
        
        player.sendMessage("§5✦ ${category.displayName} Spells ✦")
        player.sendMessage("§7${category.description}")
        player.sendMessage("")
        
        spells.forEach { spell ->
            val canCast = spell.requiredMagicLevel <= magicLevel && spell.manaCost <= playerMana
            val isSelected = selectedSpell?.id == spell.id
            val statusIcon = when {
                isSelected -> "§a✓"
                canCast -> "§e○"
                else -> "§c✗"
            }
            
            player.sendMessage("$statusIcon §d${spell.name} §7(Lv.${spell.requiredMagicLevel}, ${spell.manaCost} mana)")
            player.sendMessage("  §7${spell.description}")
        }
    }
    
    private fun showAllSpells(player: Player, magicLevel: Int, playerMana: Double) {
        player.sendMessage("§5✦ All Available Spells ✦")
        player.sendMessage("")
        
        dev.cottage.cottageRPG.spells.SpellCategory.values().forEach { category ->
            val spells = dev.cottage.cottageRPG.spells.Spell.getAllSpells()[category] ?: emptyList()
            val availableCount = spells.count { it.requiredMagicLevel <= magicLevel }
            
            player.sendMessage("§6${category.displayName}: §f$availableCount/${spells.size} available")
        }
        
        player.sendMessage("")
        player.sendMessage("§7Use §e/spells list <category> §7for detailed view")
        player.sendMessage("§7Use §e/spells §7to open the selection GUI")
    }
    
    private fun showSpellHelp(player: Player) {
        player.sendMessage("§5✦ Spell Commands ✦")
        player.sendMessage("")
        player.sendMessage("§e/spells §7- Open spell selection GUI")
        player.sendMessage("§e/spells info §7- Show your selected spells")
        player.sendMessage("§e/spells list [category] §7- List available spells")
        player.sendMessage("§e/spells cooldowns §7- Show active cooldowns")
        player.sendMessage("§e/spells select <category> <name> §7- Quick select spell")
        player.sendMessage("§e/spells clear [category] §7- Clear spell selections")
        player.sendMessage("")
        player.sendMessage("§7Categories: §fprimary, secondary, special")
        player.sendMessage("§7Casting: §fRight-click with wand/staff")
        player.sendMessage("§7  - §fNormal: Primary spells")
        player.sendMessage("§7  - §fSneak: Secondary spells")
        player.sendMessage("§7  - §fSprint: Special/Movement spells")
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (sender !is Player) return emptyList()
        
        // Check basic permission
        if (!sender.hasPermission("cottageRPG.use")) return emptyList()
        
        return when (command.name.lowercase()) {
            "rpg" -> {
                when (args.size) {
                    1 -> {
                        val subcommands = mutableListOf("stats", "skills", "class", "top", "help", "scoreboard", "admin", "spells")
                        if (sender.hasPermission("cottageRPG.admin.reload")) {
                            subcommands.add("reload")
                        }
                        subcommands.filter { it.startsWith(args[0], ignoreCase = true) }
                    }
                    2 -> {
                        when (args[0].lowercase()) {
                            "class" -> {
                                val classOptions = mutableListOf("list", "all")
                                classOptions.addAll(
                                    dev.cottage.cottageRPG.classes.RPGClass.getDefaultClasses()
                                        .map { it.id }
                                )
                                classOptions.filter { it.startsWith(args[1], ignoreCase = true) }
                            }
                            "admin" -> {
                                val adminOptions = mutableListOf("setlevel", "setskill", "setexp", "setmoney", "setclass", "resetcooldowns", "addskillexp", "addexp")
                                adminOptions.filter { it.startsWith(args[1], ignoreCase = true) }
                            }
                            else -> emptyList()
                        }
                    }
                    3 -> {
                        when (args[0].lowercase()) {
                            "admin" -> {
                                when (args[1].lowercase()) {
                                    "setlevel", "setexp", "setmoney", "addexp", "resetcooldowns" -> {
                                        plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
                                    }
                                    "setskill", "addskillexp" -> {
                                        plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
                                    }
                                    "setclass" -> {
                                        plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
                                    }
                                    else -> emptyList()
                                }
                            }
                            else -> emptyList()
                        }
                    }
                    4 -> {
                        when (args[0].lowercase()) {
                            "admin" -> {
                                when (args[1].lowercase()) {
                                    "setskill", "addskillexp" -> {
                                        val skills = Skill.getDefaultSkills().map { it.id }
                                        skills.filter { it.startsWith(args[3], ignoreCase = true) }
                                    }
                                    "setclass" -> {
                                        val classes = dev.cottage.cottageRPG.classes.RPGClass.getDefaultClasses().map { it.id }
                                        classes.filter { it.startsWith(args[3], ignoreCase = true) }
                                    }
                                    else -> emptyList()
                                }
                            }
                            else -> emptyList()
                        }
                    }
                    else -> emptyList()
                }
            }
            "class" -> {
                if (args.size == 1) {
                    val classOptions = mutableListOf("list", "all")
                    classOptions.addAll(
                        dev.cottage.cottageRPG.classes.RPGClass.getDefaultClasses()
                            .map { it.id }
                    )
                    classOptions.filter { it.startsWith(args[0], ignoreCase = true) }
                } else emptyList()
            }
            "stats", "skills" -> {
                // These commands don't need tab completion as they have no arguments
                emptyList()
            }
            "scoreboard", "sb", "rpgboard" -> {
                emptyList()
            }
            "spells" -> {
                if (args.size == 1) {
                    listOf("info", "list", "cooldowns", "select", "clear", "help").filter { 
                        it.startsWith(args[0], ignoreCase = true) 
                    }
                } else {
                    when (args[0].lowercase()) {
                        "list" -> {
                            if (args.size == 2) {
                                listOf("primary", "secondary", "special").filter { 
                                    it.startsWith(args[1], ignoreCase = true) 
                                }
                            } else emptyList()
                        }
                        "select" -> {
                            when (args.size) {
                                2 -> listOf("primary", "secondary", "special").filter { 
                                    it.startsWith(args[1], ignoreCase = true) 
                                }
                                3 -> {
                                    val category = when (args[1].lowercase()) {
                                        "primary", "p" -> dev.cottage.cottageRPG.spells.SpellCategory.PRIMARY
                                        "secondary", "s" -> dev.cottage.cottageRPG.spells.SpellCategory.SECONDARY
                                        "special", "movement", "sm" -> dev.cottage.cottageRPG.spells.SpellCategory.SPECIAL_MOVEMENT
                                        else -> return emptyList()
                                    }
                                    val rpgPlayer = plugin.playerManager.getPlayer(sender)
                                    val magicLevel = rpgPlayer.getSkillLevel("magic")
                                    plugin.playerSpellManager.getAvailableSpells(sender, category, magicLevel)
                                        .map { it.name.lowercase().replace(" ", "_") }
                                        .filter { it.startsWith(args[2], ignoreCase = true) }
                                }
                                else -> emptyList()
                            }
                        }
                        "clear" -> {
                            if (args.size == 2) {
                                listOf("primary", "secondary", "special", "all").filter { 
                                    it.startsWith(args[1], ignoreCase = true) 
                                }
                            } else emptyList()
                        }
                        else -> emptyList()
                    }
                }
            }
            else -> emptyList()
        }
    }
}
