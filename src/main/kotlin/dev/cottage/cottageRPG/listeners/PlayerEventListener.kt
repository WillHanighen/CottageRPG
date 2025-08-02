package dev.cottage.cottageRPG.listeners

import dev.cottage.cottageRPG.CottageRPG
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.*
import java.util.UUID

/**
 * Handles player events for RPG progression
 */
class PlayerEventListener(private val plugin: CottageRPG) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.playerManager.onPlayerJoin(event.player)

        val rpgPlayer = plugin.playerManager.getPlayer(event.player)
        val prefix = plugin.configManager.getString("general.prefix", "§7[§6CottageRPG§7]§r")

        // Welcome message
        event.player.sendMessage("$prefix §7Welcome back! You are level §6${rpgPlayer.level}§7.")

        if (rpgPlayer.rpgClass == null && plugin.configManager.getBoolean("classes.enabled", true)) {
            event.player.sendMessage("$prefix §7Use §6/class§7 to choose your RPG class!")
        }

        // Show scoreboard if enabled
        if (plugin.configManager.getBoolean("scoreboard.enabled", true) &&
            plugin.configManager.getBoolean("scoreboard.show_on_join", true)) {
            plugin.scoreboardManager.enableScoreboard(event.player)
        }
        
        // Update luck attribute based on enchanting skill level
        updatePlayerLuckAttribute(event.player)

        // Remove scoreboard
        //plugin.scoreboardManager.disableScoreboard(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.playerManager.onPlayerQuit(event.player)

        // Remove scoreboard
        plugin.scoreboardManager.disableScoreboard(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val block = event.block

        // Check if skills are enabled
        if (!plugin.configManager.getBoolean("skills.enabled", true)) return

        // Check if world is enabled
        val enabledWorlds = plugin.configManager.getStringList("worlds.enabled_worlds")
        if (enabledWorlds.isNotEmpty() && !enabledWorlds.contains(player.world.name)) return

        val expMultiplier = plugin.configManager.getDouble("skills.skill_exp_multiplier", 1.0)

        when (block.type) {
            // Mining skill
            Material.STONE, Material.DEEPSLATE, Material.COBBLESTONE, Material.COBBLED_DEEPSLATE,
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.NETHER_QUARTZ_ORE,
            Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS -> {
                val baseExp = when (block.type) {
                    Material.DIAMOND_ORE -> 50L
                    Material.EMERALD_ORE -> 75L
                    Material.ANCIENT_DEBRIS -> 100L
                    Material.GOLD_ORE, Material.IRON_ORE -> 25L
                    Material.COAL_ORE, Material.COPPER_ORE -> 15L
                    else -> 10L
                }
                val finalExp = (baseExp * expMultiplier).toLong()
                rpgPlayer.addSkillExperience("mining", finalExp)
                plugin.bossBarManager.showSkillXpBar(player, "mining", finalExp)
                plugin.scoreboardManager.onPlayerSkillChange(player)
            }

            // Woodcutting skill
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.WARPED_STEM, Material.CRIMSON_STEM -> {
                val finalExp = (15L * expMultiplier).toLong()
                rpgPlayer.addSkillExperience("woodcutting", finalExp)
                plugin.bossBarManager.showSkillXpBar(player, "woodcutting", finalExp)
                plugin.scoreboardManager.onPlayerSkillChange(player)
            }

            // Farming skill
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.PUMPKIN, Material.MELON, Material.SUGAR_CANE, Material.COCOA -> {
                val finalExp = (10L * expMultiplier).toLong()
                rpgPlayer.addSkillExperience("farming", finalExp)
                plugin.bossBarManager.showSkillXpBar(player, "farming", finalExp)
                plugin.scoreboardManager.onPlayerSkillChange(player)
            }

            else -> { /* No skill experience for other blocks */ }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val entity = event.entity

        // Combat skill for melee attacks
        if (damager is Player && entity is Monster) {
            val rpgPlayer = plugin.playerManager.getPlayer(damager)

            if (!plugin.configManager.getBoolean("skills.enabled", true)) return

            val enabledWorlds = plugin.configManager.getStringList("worlds.enabled_worlds")
            if (enabledWorlds.isNotEmpty() && !enabledWorlds.contains(damager.world.name)) return

            val expMultiplier = plugin.configManager.getDouble("skills.skill_exp_multiplier", 1.0)
            val baseExp = (event.finalDamage * 2).toLong()
            val finalExp = (baseExp * expMultiplier).toLong()

            rpgPlayer.addSkillExperience("combat", finalExp)
            plugin.bossBarManager.showSkillXpBar(damager, "combat", finalExp)
            plugin.scoreboardManager.onPlayerSkillChange(damager)

            // Apply combat bonuses based on skill level
            val combatLevel = rpgPlayer.getSkillLevel("combat")
            val damageBonus = combatLevel * 0.01 // 1% damage bonus per level
            event.damage = event.damage * (1.0 + damageBonus)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity

        if (entity is Player) {
            val rpgPlayer = plugin.playerManager.getPlayer(entity)

            if (!plugin.configManager.getBoolean("skills.enabled", true)) return

            val enabledWorlds = plugin.configManager.getStringList("worlds.enabled_worlds")
            if (enabledWorlds.isNotEmpty() && !enabledWorlds.contains(entity.world.name)) return

            plugin.scoreboardManager.onPlayerHealthChange(entity)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        val entity = event.entity

        if (entity is Player) {
            val rpgPlayer = plugin.playerManager.getPlayer(entity)

            if (!plugin.configManager.getBoolean("skills.enabled", true)) return

            val enabledWorlds = plugin.configManager.getStringList("worlds.enabled_worlds")
            if (enabledWorlds.isNotEmpty() && !enabledWorlds.contains(entity.world.name)) return

            plugin.scoreboardManager.onPlayerHealthChange(entity)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer

        if (killer is Player && event.entity is Monster) {
            val rpgPlayer = plugin.playerManager.getPlayer(killer)

            if (!plugin.configManager.getBoolean("skills.enabled", true)) return

            val enabledWorlds = plugin.configManager.getStringList("worlds.enabled_worlds")
            if (enabledWorlds.isNotEmpty() && !enabledWorlds.contains(killer.world.name)) return

            val expMultiplier = plugin.configManager.getDouble("player.exp_multiplier", 1.0)

            // Give main experience based on mob type
            val baseExp = when (event.entity.type) {
                org.bukkit.entity.EntityType.ZOMBIE -> 20L
                org.bukkit.entity.EntityType.SKELETON -> 25L
                org.bukkit.entity.EntityType.CREEPER -> 30L
                org.bukkit.entity.EntityType.SPIDER -> 15L
                org.bukkit.entity.EntityType.ENDERMAN -> 50L
                org.bukkit.entity.EntityType.WITHER_SKELETON -> 100L
                org.bukkit.entity.EntityType.BLAZE -> 75L
                else -> 10L
            }

            val leveledUp = rpgPlayer.addExperience((baseExp * expMultiplier).toLong())

            // Show boss bar for main experience gain
            plugin.bossBarManager.showMainLevelXpBar(killer, (baseExp * expMultiplier).toLong())

            if (leveledUp) {
                // Show level up effects
                if (plugin.configManager.getBoolean("particles.enabled", true)) {
                    val particle = org.bukkit.Particle.valueOf(
                        plugin.configManager.getString("particles.level_up_particle", "FIREWORK")
                    )
                    killer.world.spawnParticle(particle, killer.location.add(0.0, 1.0, 0.0), 20)
                }
            }
            plugin.scoreboardManager.onPlayerLevelChange(killer)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerFish(event: PlayerFishEvent) {
        if (event.state == PlayerFishEvent.State.CAUGHT_FISH) {
            val player = event.player
            val rpgPlayer = plugin.playerManager.getPlayer(player)

            if (!plugin.configManager.getBoolean("skills.enabled", true)) return

            val enabledWorlds = plugin.configManager.getStringList("worlds.enabled_worlds")
            if (enabledWorlds.isNotEmpty() && !enabledWorlds.contains(player.world.name)) return

            val expMultiplier = plugin.configManager.getDouble("skills.skill_exp_multiplier", 1.0)
            val finalExp = (25L * expMultiplier).toLong()
            rpgPlayer.addSkillExperience("fishing", finalExp)
            plugin.bossBarManager.showSkillXpBar(player, "fishing", finalExp)
            plugin.scoreboardManager.onPlayerSkillChange(player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val rpgPlayer = plugin.playerManager.getPlayer(player)

        // Restore RPG health after respawn
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            player.maxHealth = rpgPlayer.maxHealth
            player.health = rpgPlayer.maxHealth
        }, 1L)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerLevelChange(event: PlayerLevelChangeEvent) {
        val player = event.player
        val rpgPlayer = plugin.playerManager.getPlayer(player)

        // Sync Bukkit experience with RPG experience display
        updatePlayerExperience(player)

        // Update scoreboard when player's Bukkit level changes
        plugin.scoreboardManager.onPlayerLevelChange(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerExpChange(event: PlayerExpChangeEvent) {
        val player = event.player

        // Update scoreboard when player gains experience
        plugin.scoreboardManager.onPlayerExperienceGain(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        val hitEntity = event.hitEntity

        // Check if it's an arrow shot by a player hitting a living entity
        if (projectile is Arrow && projectile.shooter is Player && hitEntity is LivingEntity) {
            val shooter = projectile.shooter as Player
            val rpgPlayer = plugin.playerManager.getPlayer(shooter)

            if (!plugin.configManager.getBoolean("skills.enabled", true)) return

            val enabledWorlds = plugin.configManager.getStringList("worlds.enabled_worlds")
            if (enabledWorlds.isNotEmpty() && !enabledWorlds.contains(shooter.world.name)) return

            // Calculate shot distance
            val shooterLocation = shooter.location
            val hitLocation = hitEntity.location
            val distance = shooterLocation.distance(hitLocation)

            // Base XP for hitting an entity
            val baseExp = when (hitEntity) {
                is Monster -> 10L // More XP for hitting monsters
                is Player -> 5L  // Less XP for hitting players (PvP)
                else -> 3L       // Base XP for other living entities
            }

            // Distance bonus: base XP * (distance / 10)
            val distanceMultiplier = (distance / 15.0).coerceAtLeast(1.0) // Minimum 1x multiplier
            val totalExp = (baseExp * distanceMultiplier).toLong().coerceIn(3L, 100L)

            // Apply skill experience multiplier from config
            val expMultiplier = plugin.configManager.getDouble("skills.skill_exp_multiplier", 1.0)
            val finalExp = (totalExp * expMultiplier).toLong()

            rpgPlayer.addSkillExperience("archery", finalExp)

            // Show boss bar for archery skill gain
            plugin.bossBarManager.showSkillXpBar(shooter, "archery", finalExp)

            // Send feedback to player about the shot
            val distanceFormatted = String.format("%.1f", distance)
            shooter.playSound(shooter.location, org.bukkit.Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f)
            if (finalExp == 100.toLong()) {
                shooter.sendMessage("§6Archery XP: +$finalExp (${distanceFormatted}m shot) §c(MAX)")
            } else {
                shooter.sendMessage("§6Archery XP: +$finalExp (${distanceFormatted}m shot)")
            }

            // Delay health check by 1 tick to get health after damage is applied
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                shooter.sendMessage("§6Entity Health: §c${Math.floor(hitEntity.health * 2) / 2.0}/${hitEntity.maxHealth}")
            }, 1L)

            // Update scoreboard immediately
            plugin.scoreboardManager.onPlayerSkillChange(shooter)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEnchantItem(event: EnchantItemEvent) {
        val player = event.enchanter
        val rpgPlayer = plugin.playerManager.getPlayer(player)

        if (!plugin.configManager.getBoolean("skills.enabled", true)) return

        val enabledWorlds = plugin.configManager.getStringList("worlds.enabled_worlds")
        if (enabledWorlds.isNotEmpty() && !enabledWorlds.contains(player.world.name)) return

        // Calculate XP based on enchantment level and cost
        val enchantmentCount = event.enchantsToAdd.size
        val totalLevels = event.enchantsToAdd.values.sum()
        val expLevelCost = event.expLevelCost
        
        // Base XP calculation: 10 XP per enchantment level + 5 XP per experience level cost
        val baseExp = (totalLevels * 10) + (expLevelCost * 5)
        val expMultiplier = plugin.configManager.getDouble("skills.skill_exp_multiplier", 1.0)
        val finalExp = (baseExp * expMultiplier).toLong()

        rpgPlayer.addSkillExperience("enchanting", finalExp)
        plugin.bossBarManager.showSkillXpBar(player, "enchanting", finalExp)
        plugin.scoreboardManager.onPlayerSkillChange(player)
        
        // Update luck attribute based on new enchanting level
        updatePlayerLuckAttribute(player)
    }

    /**
     * Update player's luck attribute based on enchanting skill level
     */
    private fun updatePlayerLuckAttribute(player: Player) {
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val enchantingLevel = rpgPlayer.getSkillLevel("enchanting")
        
        // Remove existing enchanting luck modifier
        val luckAttribute = player.getAttribute(Attribute.LUCK)
        luckAttribute?.let { attribute ->
            // Remove any existing enchanting luck modifier
            val existingModifier = attribute.modifiers.find { it.name == "CottageRPG_Enchanting_Luck" }
            existingModifier?.let { attribute.removeModifier(it) }
            
            // Add new luck modifier based on enchanting level
            if (enchantingLevel > 0) {
                // Each enchanting level gives 0.1 luck (max +10 luck at level 100)
                val luckBonus = enchantingLevel * 0.1
                val modifier = AttributeModifier(
                    UUID.nameUUIDFromBytes("CottageRPG_Enchanting_Luck".toByteArray()),
                    "CottageRPG_Enchanting_Luck",
                    luckBonus,
                    AttributeModifier.Operation.ADD_NUMBER
                )
                attribute.addModifier(modifier)
                
                // Notify player of luck bonus
                if (plugin.configManager.getBoolean("skills.show_attribute_updates", true)) {
                    player.sendMessage("§6[Enchanting] §7Your luck has increased! §a+${String.format("%.1f", luckBonus)} Luck")
                }
            }
        }
    }

    private fun updatePlayerExperience(player: Player) {
        val rpgPlayer = plugin.playerManager.getPlayer(player)

        // Calculate experience progress for display
        val expToNext = rpgPlayer.getExperienceForNextLevel()
        val currentExp = rpgPlayer.experience % expToNext
        val progress = if (expToNext > 0) currentExp.toFloat() / expToNext else 1.0f

        player.exp = progress
        player.level = rpgPlayer.level
    }
}
