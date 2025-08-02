package dev.cottage.cottageRPG.commands

import dev.cottage.cottageRPG.CottageRPG
import dev.cottage.cottageRPG.gui.PlayerStatsGUI
import dev.cottage.cottageRPG.gui.ClassSelectionGUI
import dev.cottage.cottageRPG.gui.SkillsGUI
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
                        "top" -> {
                            showTopPlayers(sender)
                        }
                        "help" -> {
                            showHelp(sender)
                        }
                        "scoreboard", "sb" -> {
                            toggleScoreboard(sender)
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
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (sender !is Player) return emptyList()
        
        // Check basic permission
        if (!sender.hasPermission("cottageRPG.use")) return emptyList()
        
        return when (command.name.lowercase()) {
            "rpg" -> {
                when (args.size) {
                    1 -> {
                        val subcommands = mutableListOf("stats", "skills", "class", "top", "help", "scoreboard")
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
            else -> emptyList()
        }
    }
}
