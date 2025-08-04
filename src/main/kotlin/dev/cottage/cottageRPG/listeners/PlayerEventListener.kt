package dev.cottage.cottageRPG.listeners

import dev.cottage.cottageRPG.CottageRPG
import dev.cottage.cottageRPG.skills.Skill
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Arrow
import org.bukkit.entity.AbstractArrow
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
import org.bukkit.event.block.Action
import dev.cottage.cottageRPG.gui.SpellSelectionGUI
import dev.cottage.cottageRPG.spells.SpellCategory
import dev.cottage.cottageRPG.spells.SpellCastResult
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.Sound
import org.bukkit.Particle
import org.bukkit.Location
import org.bukkit.util.Vector
import org.bukkit.entity.Fireball
import org.bukkit.entity.Snowball
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.block.Block
import kotlin.random.Random
import org.bukkit.scheduler.BukkitRunnable

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
            val item = damager.inventory.itemInMainHand

            if (!plugin.configManager.getBoolean("skills.enabled", true)) return

            val enabledWorlds = plugin.configManager.getStringList("worlds.enabled_worlds")
            if (enabledWorlds.isNotEmpty() && !enabledWorlds.contains(damager.world.name)) return

            // Check if attacking with a magic wand - cast spells instead of normal combat
            if (plugin.wandCraftingManager.isMagicWand(item)) {
                // Cancel the normal attack damage
                event.isCancelled = true

                // Cast spell based on player state
                val spellCategory = when {
                    damager.isSneaking -> dev.cottage.cottageRPG.spells.SpellCategory.SECONDARY
                    else -> dev.cottage.cottageRPG.spells.SpellCategory.PRIMARY
                }

                // Get selected spell for the category
                val spell = plugin.playerSpellManager.getSelectedSpell(damager, spellCategory)
                if (spell == null) {
                    damager.sendMessage("§cNo spell selected for ${spellCategory.displayName} category! Use /spells to select one.")
                    return
                }

                // Check if player can cast the spell
                val magicLevel = rpgPlayer.getSkillLevel("magic")
                val playerMana = rpgPlayer.mana
                val castResult = spell.canCast(damager, magicLevel, playerMana, plugin.spellCooldownManager)

                when (castResult) {
                    dev.cottage.cottageRPG.spells.SpellCastResult.INSUFFICIENT_LEVEL -> {
                        damager.sendMessage("§cYou need Magic level ${spell.requiredMagicLevel} to cast ${spell.name}!")
                        return
                    }
                    dev.cottage.cottageRPG.spells.SpellCastResult.INSUFFICIENT_MANA -> {
                        damager.sendMessage("§cNot enough mana! Need ${spell.manaCost} mana to cast ${spell.name}.")
                        return
                    }
                    dev.cottage.cottageRPG.spells.SpellCastResult.ON_COOLDOWN -> {
                        val remaining = plugin.spellCooldownManager.getRemainingCooldown(damager, spell.id)
                        damager.sendMessage("§c${spell.name} is on cooldown for ${remaining}s!")
                        return
                    }
                    dev.cottage.cottageRPG.spells.SpellCastResult.SUCCESS -> {
                        // Cast the spell with target entity context
                        castSpellOnTarget(damager, spell, rpgPlayer, entity)
                    }
                    else -> {
                        damager.sendMessage("§cFailed to cast ${spell.name}!")
                        return
                    }
                }
                return
            }

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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity

        if (entity is Player) {
            val rpgPlayer = plugin.playerManager.getPlayer(entity)

            if (!plugin.configManager.getBoolean("skills.enabled", true)) return

            val enabledWorlds = plugin.configManager.getStringList("worlds.enabled_worlds")
            if (enabledWorlds.isNotEmpty() && !enabledWorlds.contains(entity.world.name)) return

            // Handle mana shield damage conversion
            if (activeManaShields.containsKey(entity)) {
                val originalDamage = event.damage
                val modifiedDamage = handleManaShieldDamage(entity, originalDamage)
                event.damage = modifiedDamage
            }

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

    @EventHandler(priority = EventPriority.MONITOR)
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

            // Distance bonus (1 XP per 10 blocks)
            val distanceBonus = (distance / 10).toLong()
            val totalExp = baseExp + distanceBonus

            val expMultiplier = plugin.configManager.getDouble("skills.skill_exp_multiplier", 1.0)
            val finalExp = (totalExp * expMultiplier).toLong()

            rpgPlayer.addSkillExperience("archery", finalExp)
            plugin.bossBarManager.showSkillXpBar(shooter, "archery", finalExp)

            // Show hit feedback
            shooter.sendMessage("§6Hit! §7Distance: §e${String.format("%.1f", distance)}m §7XP: §a+$finalExp")

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

    // Magic Wand item
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand
        val action = event.action

        // Only allow magic wands for spell casting
        val isSpellItem = plugin.wandCraftingManager.isMagicWand(item)

        if (!isSpellItem) return

        // Handle all spell casting items (including magic wands) in one place
        when (action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                // Handle spell casting with right-click
                handleSpellCasting(event)
            }
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                event.isCancelled = true

                if (player.isSneaking) {
                    // Open spell selection GUI
                    SpellSelectionGUI(plugin).open(player)
                } else {
                    // Cast special/movement spells with left-click
                    val rpgPlayer = plugin.playerManager.getPlayer(player)
                    val spell = plugin.playerSpellManager.getSelectedSpell(player, SpellCategory.SPECIAL_MOVEMENT)

                    if (spell == null) {
                        player.sendMessage("§cNo spell selected for Special/Movement category! Use /spells to select one.")
                        return
                    }

                    // Check if player can cast the spell
                    val magicLevel = rpgPlayer.getSkillLevel("magic")
                    val playerMana = rpgPlayer.mana
                    val castResult = spell.canCast(player, magicLevel, playerMana, plugin.spellCooldownManager)

                    when (castResult) {
                        SpellCastResult.INSUFFICIENT_LEVEL -> {
                            player.sendMessage("§cYou need Magic level ${spell.requiredMagicLevel} to cast ${spell.name}!")
                            return
                        }
                        SpellCastResult.INSUFFICIENT_MANA -> {
                            player.sendMessage("§cNot enough mana! Need ${spell.manaCost} mana to cast ${spell.name}.")
                            return
                        }
                        SpellCastResult.ON_COOLDOWN -> {
                            val remaining = plugin.spellCooldownManager.getRemainingCooldown(player, spell.id)
                            player.sendMessage("§c${spell.name} is on cooldown for ${remaining}s!")
                            return
                        }
                        SpellCastResult.SUCCESS -> {
                            // Cast the spell
                            castSpell(player, spell, rpgPlayer)
                        }
                        else -> {
                            player.sendMessage("§cFailed to cast ${spell.name}!")
                            return
                        }
                    }
                }
            }
            else -> {}
        }
    }

    private fun handleSpellCasting(event: PlayerInteractEvent) {
        val player = event.player
        val action = event.action

        // Only handle right-click actions and avoid double events
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return

        val item = player.inventory.itemInMainHand
        if (item.type == Material.AIR) return

        // Only allow magic wands for spell casting
        val isSpellItem = plugin.wandCraftingManager.isMagicWand(item)

        if (!isSpellItem) return

        val rpgPlayer = plugin.playerManager.getPlayer(player)

        // Determine spell category based on interaction type
        val spellCategory = when {
            player.isSneaking -> SpellCategory.SECONDARY
            // Sprint + Right-click = PRIMARY (for attacking while sprinting)
            // Sprint + Left-click = SPECIAL_MOVEMENT (handled separately in onPlayerInteract)
            else -> SpellCategory.PRIMARY
        }

        // Get selected spell for the category
        val spell = plugin.playerSpellManager.getSelectedSpell(player, spellCategory)
        if (spell == null) {
            player.sendMessage("§cNo spell selected for ${spellCategory.displayName} category! Use /spells to select one.")
            return
        }

        // Check if player can cast the spell
        val magicLevel = rpgPlayer.getSkillLevel("magic")
        val playerMana = rpgPlayer.mana
        val castResult = spell.canCast(player, magicLevel, playerMana, plugin.spellCooldownManager)

        when (castResult) {
            SpellCastResult.INSUFFICIENT_LEVEL -> {
                player.sendMessage("§cYou need Magic level ${spell.requiredMagicLevel} to cast ${spell.name}!")
                return
            }
            SpellCastResult.INSUFFICIENT_MANA -> {
                player.sendMessage("§cNot enough mana! Need ${spell.manaCost} mana to cast ${spell.name}.")
                return
            }
            SpellCastResult.ON_COOLDOWN -> {
                val remaining = plugin.spellCooldownManager.getRemainingCooldown(player, spell.id)
                player.sendMessage("§c${spell.name} is on cooldown for ${remaining}s!")
                return
            }
            SpellCastResult.SUCCESS -> {
                // Cast the spell
                castSpell(player, spell, rpgPlayer)
                event.isCancelled = true
            }
            else -> {
                player.sendMessage("§cFailed to cast ${spell.name}!")
                return
            }
        }
    }

    /**
     * Cast a spell and handle all effects
     */
    private fun castSpell(player: Player, spell: dev.cottage.cottageRPG.spells.Spell, rpgPlayer: dev.cottage.cottageRPG.player.RPGPlayer) {
        // First validate if the spell can be executed
        val validationResult = validateSpellExecution(player, spell)
        if (!validationResult.canExecute) {
            // Spell failed validation - don't consume mana or set cooldown
            player.sendMessage("§c✗ ${spell.name} failed: §7${validationResult.failureReason}")
            player.world.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f)
            return
        }

        // Consume mana only after validation passes
        rpgPlayer.mana -= spell.manaCost

        // Set cooldown with magic level reduction
        val magicLevel = rpgPlayer.getSkillLevel("magic")
        plugin.spellCooldownManager.setCooldownWithReduction(player, spell.id, spell.cooldownSeconds, magicLevel)

        // Add magic skill experience
        val expGain = (spell.manaCost * 0.5).toLong()
        rpgPlayer.addSkillExperience("magic", expGain)
        plugin.bossBarManager.showSkillXpBar(player, "magic", expGain)

        // Play casting sound
        player.world.playSound(player.location, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f)

        // Execute spell effect (now guaranteed to succeed)
        executeSpellEffect(player, spell)

        // Show casting message with cooldown info
        val actualCooldown = plugin.spellCooldownManager.getRemainingCooldown(player, spell.id)
        player.sendMessage("§5✦ §dCast ${spell.name}! §7(${spell.manaCost} mana, ${actualCooldown}s cooldown)")

        // Update scoreboard
        plugin.scoreboardManager.onPlayerSkillChange(player)
    }

    /**
     * Data class to hold spell validation results
     */
    private data class SpellValidationResult(
        val canExecute: Boolean,
        val failureReason: String = ""
    )

    /**
     * Validate if a spell can be executed before consuming resources
     */
    private fun validateSpellExecution(player: Player, spell: dev.cottage.cottageRPG.spells.Spell): SpellValidationResult {
        return when (spell.id) {
            "teleport" -> {
                val target = player.getTargetBlock(null, 50)
                if (target.type == Material.AIR) {
                    SpellValidationResult(false, "No valid teleport target found within range")
                } else {
                    SpellValidationResult(true)
                }
            }
            "light" -> {
                val target = player.getTargetBlock(null, 20)
                if (target.type == Material.AIR) {
                    SpellValidationResult(false, "No solid surface found to place light within range")
                } else {
                    SpellValidationResult(true)
                }
            }
            "lightning_bolt" -> {
                val target = player.getTargetBlock(null, 30)
                if (target.type == Material.AIR) {
                    SpellValidationResult(false, "No target found within range for lightning strike")
                } else {
                    SpellValidationResult(true)
                }
            }
            "barrier" -> {
                val target = player.getTargetBlock(null, 15)
                if (target.type == Material.AIR) {
                    SpellValidationResult(false, "No location found to create barrier within range")
                } else {
                    SpellValidationResult(true)
                }
            }
            // Spells that always succeed (self-targeting or guaranteed effects)
            "fireball", "ice_shard", "magic_missile", "shield", "heal", "mana_shield",
            "invisibility", "jump_boost", "speed", "levitation", "dash" -> {
                SpellValidationResult(true)
            }
            else -> {
                // Unknown spell - allow execution but log warning
                SpellValidationResult(true)
            }
        }
    }

    /**
     * Execute the specific effect of a spell
     */
    private fun executeSpellEffect(player: Player, spell: dev.cottage.cottageRPG.spells.Spell) {
        when (spell.id) {
            // Primary Spells
            "fireball" -> castFireball(player)
            "lightning_bolt" -> castLightningBolt(player)
            "ice_shard" -> castIceShard(player)
            "magic_missile" -> castMagicMissile(player)

            // Secondary Spells
            "shield" -> castShield(player)
            "heal" -> castHeal(player)
            "barrier" -> castBarrier(player)
            "mana_shield" -> castManaShield(player)

            // Special/Movement Spells
            "light" -> castLight(player)
            "teleport" -> castTeleport(player)
            "invisibility" -> castInvisibility(player)
            "jump_boost" -> castJumpBoost(player)
            "speed" -> castSpeed(player)
            "levitation" -> castLevitation(player)
            "dash" -> castDash(player)
        }
    }

    // Primary Spell Effects
    private fun castFireball(player: Player) {
        val fireball = player.world.spawn(player.eyeLocation, Fireball::class.java)
        fireball.direction = player.location.direction
        fireball.velocity = player.location.direction.multiply(2.0)
        fireball.shooter = player
        fireball.yield = 2.5f

        // effects
        player.world.spawnParticle(Particle.FLAME, player.eyeLocation, 10, 0.3, 0.3, 0.3, 0.1)
        player.world.playSound(player.location, Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.2f)
        player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, 120, 0))
        player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 5, 3))
    }

    private fun castLightningBolt(player: Player) {
        val target = player.getTargetBlock(null, 30)
        if (target.type != Material.AIR) {
            player.world.strikeLightning(target.location.add(0.0, 1.0, 0.0))
            player.world.spawnParticle(Particle.ELECTRIC_SPARK, target.location.add(0.5, 1.0, 0.5), 20, 0.5, 1.0, 0.5, 0.2)
        }
    }

    private fun castIceShard(player: Player) {
        val snowball = player.world.spawn(player.eyeLocation, Snowball::class.java)
        snowball.velocity = player.location.direction.multiply(2.5)
        snowball.shooter = player

        // Ice particle effects
        player.world.spawnParticle(Particle.SNOWFLAKE, player.eyeLocation, 15, 0.3, 0.3, 0.3, 0.1)
        player.world.playSound(player.location, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f)
    }

    private fun castMagicMissile(player: Player) {
        val baseDirection = player.location.direction
        val random = Random.Default

        // Spawn 6 arrows with slight delays and proper spread
        for (i in 0 until 8) {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val arrow = player.world.spawnArrow(player.eyeLocation, baseDirection, 3.0f, 0.0f)

                // Create random spread in 3D space
                val spreadAmount = 0.3 // Adjust this value to increase/decrease spread

                // Add random offsets to each direction component
                val randomX = (random.nextDouble() - 0.5) * spreadAmount * 0.2
                val randomY = (random.nextDouble() - 0.5) * spreadAmount * 0.2 // Less vertical spread
                val randomZ = (random.nextDouble() - 0.5) * spreadAmount * 0.2

                val newDirection = baseDirection.clone()
                newDirection.x += randomX
                newDirection.y += randomY
                newDirection.z += randomZ

                // Normalize the direction vector to maintain consistent speed
                newDirection.normalize()

                arrow.velocity = newDirection.multiply(3.0)
                arrow.shooter = player
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED) // Prevent pickup
                arrow.setGravity(false)

                // Magic particle effects for each arrow
                player.world.spawnParticle(Particle.ENCHANT, arrow.location, 5, 0.1, 0.1, 0.1, 0.05)

                // Remove arrow after 5 seconds to prevent clutter
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    if (!arrow.isDead) {
                        arrow.remove()
                    }
                }, 100L) // 5 seconds = 100 ticks

            }, (i * 1L)) // 1 tick delay between each arrow
        }

        // Initial sound and particle effect
        player.world.spawnParticle(Particle.ENCHANT, player.eyeLocation, 10, 0.2, 0.2, 0.2, 0.1)
        player.world.playSound(player.location, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.8f)
    }

    // Secondary Spells
    private fun castShield(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 200, 1)) // 10 seconds
        player.world.spawnParticle(Particle.ENCHANT, player.location.add(0.0, 1.0, 0.0), 30, 1.0, 1.0, 1.0, 0.1)
        player.world.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f)
        player.sendMessage("§9Shield activated! Reduced damage for 10 seconds.")
    }

    private fun castHeal(player: Player) {
        val healPerSecond = 2.0 // 1 heart per second
        val maxHealAmount = 10.0 // 5 hearts maximum
        val currentHealth = player.health
        val maxHealth = player.maxHealth

        // Calculate how much we can actually heal (limited by max heal amount and current health)
        val healthDeficit = maxHealth - currentHealth
        val actualMaxHeal = minOf(maxHealAmount, healthDeficit)

        if (actualMaxHeal <= 0) {
            player.sendMessage("§cYou are already at full health!")
            return
        }

        // Calculate duration based on heal amount (2 HP per second)
        val healDuration = (actualMaxHeal / healPerSecond).toLong() * 20L // Convert to ticks (20 ticks = 1 second)
        val ticksPerHeal = 20L // Heal every second (20 ticks)
        var totalHealed = 0.0

        player.world.spawnParticle(Particle.HEART, player.location.add(0.0, 1.0, 0.0), 5, 0.5, 0.5, 0.5, 0.1)
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f)
        player.sendMessage("§aHealing over time started! Regenerating ${roundToNearestHalf(actualMaxHeal/2)} hearts...")

        // Create a repeating task that heals every second
        val task = object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline || player.isDead) {
                    cancel()
                    return
                }

                val currentPlayerHealth = player.health
                if (currentPlayerHealth >= player.maxHealth || totalHealed >= actualMaxHeal) {
                    player.sendMessage("§aHealing complete! Total healed: ${roundToNearestHalf(totalHealed/2)} hearts")
                    cancel()
                    return
                }

                // Heal 2 HP (1 heart) this tick
                val healThisTick = minOf(healPerSecond, actualMaxHeal - totalHealed, player.maxHealth - currentPlayerHealth)
                player.health = (currentPlayerHealth + healThisTick).coerceAtMost(player.maxHealth)
                totalHealed += healThisTick

                // Spawn healing particles
                player.world.spawnParticle(Particle.HEART, player.location.add(0.0, 1.0, 0.0), 3, 0.3, 0.3, 0.3, 0.05)
                player.world.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f)
            }
        }

        task.runTaskTimer(plugin, 0L, ticksPerHeal)
    }

    private fun castBarrier(player: Player) {
        val location = player.getTargetBlock(null, 10).location
        createBarrierWall(location, player.location.direction)

        player.world.spawnParticle(Particle.BLOCK, location.add(0.5, 1.0, 0.5), 20, 2.0, 2.0, 2.0, 0.1)
        player.world.playSound(location, Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f)
        player.sendMessage("§7Barrier created!")
    }

    private val activeManaShields = mutableMapOf<Player, Long>() // Player to expiration time

    private fun castManaShield(player: Player) {
        val duration = 20000L // 20 seconds in milliseconds
        val expirationTime = System.currentTimeMillis() + duration

        // Add player to active shields
        activeManaShields[player] = expirationTime

        // Visual and audio effects
        player.world.spawnParticle(Particle.ENCHANT, player.location.add(0.0, 1.0, 0.0), 30, 1.0, 1.5, 1.0, 0.2)
        player.world.spawnParticle(Particle.PORTAL, player.location.add(0.0, 1.0, 0.0), 15, 0.8, 1.0, 0.8, 0.1)
        player.world.playSound(player.location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f)
        player.world.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f)

        // Give slight resistance to show the shield is active
        player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 400, 0, false, false)) // 20 seconds, level 1

        player.sendMessage("§b✦ Mana Shield activated! §7Damage will drain mana instead of health for §b20 seconds§7.")

        // Schedule shield expiration check
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (activeManaShields.containsKey(player)) {
                activeManaShields.remove(player)
                player.sendMessage("§7Your Mana Shield has expired.")
                player.world.spawnParticle(Particle.SMOKE, player.location.add(0.0, 1.0, 0.0), 10, 0.5, 0.5, 0.5, 0.05)
            }
        }, 400L) // 20 seconds
    }

    // Special/Movement Spells
    private fun castLight(player: Player) {
        val target = player.getTargetBlock(null, 20)
        if (target.type != Material.AIR) {
            val lightLocation = target.location.add(0.0, 1.0, 0.0)
            lightLocation.block.type = Material.LIGHT

            player.world.spawnParticle(Particle.GLOW, lightLocation.add(0.5, 0.0, 0.5), 15, 0.3, 0.3, 0.3, 0.1)
            player.world.playSound(lightLocation, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f)

            // Remove light after 60 seconds
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                if (lightLocation.block.type == Material.LIGHT) {
                    lightLocation.block.type = Material.AIR
                }
            }, 1200L)
        }
    }

    private fun castTeleport(player: Player) {
        val target = player.getTargetBlock(null, 50)
        val teleportLocation = target.location.add(0.5, 1.0, 0.5)
        teleportLocation.yaw = player.location.yaw
        teleportLocation.pitch = player.location.pitch

        // Particle effects at both locations
        player.world.spawnParticle(Particle.PORTAL, player.location.add(0.0, 1.0, 0.0), 30, 0.5, 1.0, 0.5, 0.1)
        player.world.spawnParticle(Particle.PORTAL, teleportLocation, 30, 0.5, 1.0, 0.5, 0.1)

        player.teleport(teleportLocation)
        player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
        player.sendMessage("§5Teleported!")
    }

    private fun castInvisibility(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 300, 0)) // 15 seconds
        player.world.spawnParticle(Particle.SMOKE, player.location.add(0.0, 1.0, 0.0), 20, 0.5, 1.0, 0.5, 0.1)
        player.world.playSound(player.location, Sound.ENTITY_BAT_TAKEOFF, 1.0f, 0.8f)
        player.sendMessage("§7You fade from sight...")
    }

    private fun castJumpBoost(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, 200, 2)) // 10 seconds, level 3
        player.world.spawnParticle(Particle.CLOUD, player.location, 15, 0.5, 0.1, 0.5, 0.1)
        player.world.playSound(player.location, Sound.ENTITY_RABBIT_JUMP, 1.0f, 1.2f)
        player.sendMessage("§fYour legs feel lighter!")
    }

    private fun castSpeed(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 200, 1)) // 10 seconds, level 2
        player.world.spawnParticle(Particle.SWEEP_ATTACK, player.location.add(0.0, 1.0, 0.0), 10, 1.0, 0.5, 1.0, 0.1)
        player.world.playSound(player.location, Sound.ENTITY_HORSE_GALLOP, 1.0f, 1.5f)
        player.sendMessage("§eYou feel swift!")
    }

    private fun castLevitation(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.LEVITATION, 100, 3)) // 5 seconds
        player.world.spawnParticle(Particle.END_ROD, player.location.add(0.0, 1.0, 0.0), 20, 0.3, 0.5, 0.3, 0.1)
        player.world.playSound(player.location, Sound.ENTITY_SHULKER_SHOOT, 1.0f, 1.2f)
        player.sendMessage("§dYou begin to float...")
    }

    private fun castDash(player: Player) {
        val direction = player.location.direction.multiply(1.5)
        direction.y = 0.3 // Add slight upward momentum
        player.velocity = direction

        player.world.spawnParticle(Particle.SWEEP_ATTACK, player.location, 5, 0.5, 0.1, 0.5, 0.1)
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f)
        player.sendMessage("§6Dash!")
    }

    /**
     * Create a temporary barrier wall
     */
    private fun createBarrierWall(center: Location, direction: Vector) {
        val perpendicular = Vector(-direction.z, 0.0, direction.x).normalize()
        val blocks = mutableListOf<Block>()

        // Create a 5x3 wall
        for (x in -2..2) {
            for (y in 0..2) {
                val blockLocation = center.clone().add(perpendicular.clone().multiply(x.toDouble())).add(0.0, y.toDouble(), 0.0)
                val block = blockLocation.block
                if (block.type == Material.AIR) {
                    block.type = Material.GLASS
                    blocks.add(block)
                }
            }
        }

        // Remove barrier after 10 seconds
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            blocks.forEach { block ->
                if (block.type == Material.GLASS) {
                    block.type = Material.AIR
                }
            }
        }, 200L)
    }

    /**
     * Update player's luck attribute based on enchanting skill level
     */
    private fun updatePlayerLuckAttribute(player: Player) {
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val enchantingLevel = rpgPlayer.getSkillLevel("enchanting")

        // Get luck attribute
        val luckAttribute = player.getAttribute(Attribute.LUCK) ?: return

        // Define the modifier UUID and name consistently
        val modifierUUID = UUID.nameUUIDFromBytes("CottageRPG_Enchanting_Luck".toByteArray())
        val modifierName = "CottageRPG_Enchanting_Luck"

        // Remove any existing enchanting luck modifiers (check by both UUID and name)
        val existingModifiers = luckAttribute.modifiers.filter {
            it.uniqueId == modifierUUID || it.name == modifierName
        }

        existingModifiers.forEach { modifier ->
            try {
                luckAttribute.removeModifier(modifier)
            } catch (e: Exception) {
                // Log but don't fail if removal fails
                plugin.logger.warning("Failed to remove existing luck modifier: ${e.message}")
            }
        }

        // Add new luck modifier based on enchanting level
        if (enchantingLevel > 0) {
            try {
                // Each enchanting level gives 0.1 luck (max +10 luck at level 100)
                val luckBonus = enchantingLevel * 0.1
                val modifier = AttributeModifier(
                    modifierUUID,
                    modifierName,
                    luckBonus,
                    AttributeModifier.Operation.ADD_NUMBER
                )

                // Double-check that the modifier doesn't already exist before adding
                val stillExists = luckAttribute.modifiers.any {
                    it.uniqueId == modifierUUID || it.name == modifierName
                }

                if (!stillExists) {
                    luckAttribute.addModifier(modifier)

                    // Notify player of luck bonus
                    if (plugin.configManager.getBoolean("skills.show_attribute_updates", true)) {
                        player.sendMessage("§6[Enchanting] §7Your luck has increased! §a+${String.format("%.1f", luckBonus)} Luck")
                    }
                } else {
                    plugin.logger.warning("Luck modifier still exists after removal attempt for player ${player.name}")
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to add luck modifier for player ${player.name}: ${e.message}")
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

    /**
     * Cast a spell and handle all effects
     */
    private fun castSpellOnTarget(player: Player, spell: dev.cottage.cottageRPG.spells.Spell, rpgPlayer: dev.cottage.cottageRPG.player.RPGPlayer, target: org.bukkit.entity.Entity) {
        // First validate if the spell can be executed
        val validationResult = validateSpellExecution(player, spell)
        if (!validationResult.canExecute) {
            // Spell failed validation - don't consume mana or set cooldown
            player.sendMessage("§c✗ ${spell.name} failed: §7${validationResult.failureReason}")
            player.world.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f)
            return
        }

        // Consume mana only after validation passes
        rpgPlayer.mana -= spell.manaCost

        // Set cooldown with magic level reduction
        val magicLevel = rpgPlayer.getSkillLevel("magic")
        plugin.spellCooldownManager.setCooldownWithReduction(player, spell.id, spell.cooldownSeconds, magicLevel)

        // Add magic skill experience
        val expGain = (spell.manaCost * 0.5).toLong()
        rpgPlayer.addSkillExperience("magic", expGain)
        plugin.bossBarManager.showSkillXpBar(player, "magic", expGain)

        // Play casting sound
        player.world.playSound(player.location, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f)

        // Execute spell effect with target context
        executeSpellEffectOnTarget(player, spell, target)

        // Show casting message with cooldown info
        val actualCooldown = plugin.spellCooldownManager.getRemainingCooldown(player, spell.id)
        player.sendMessage("§5✦ §dCast ${spell.name} on target! §7(${spell.manaCost} mana, ${actualCooldown}s cooldown)")

        // Update scoreboard
        plugin.scoreboardManager.onPlayerSkillChange(player)
    }

    /**
     * Execute spell effects with target entity context
     */
    private fun executeSpellEffectOnTarget(player: Player, spell: dev.cottage.cottageRPG.spells.Spell, target: org.bukkit.entity.Entity) {
        when (spell.id) {
            // Primary Spells - Direct damage/effects on target
            "fireball" -> castFireballOnTarget(player, target)
            "lightning_bolt" -> castLightningBoltOnTarget(player, target)
            "ice_shard" -> castIceShardOnTarget(player, target)
            "magic_missile" -> castMagicMissileOnTarget(player, target)

            // Secondary Spells - Self-cast (same as regular casting)
            "shield" -> castShield(player)
            "heal" -> castHeal(player)
            "barrier" -> castBarrier(player)
            "mana_shield" -> castManaShield(player)

            // Special/Movement Spells - Self-cast (same as regular casting)
            "light" -> castLight(player)
            "teleport" -> castTeleportToTarget(player, target)
            "invisibility" -> castInvisibility(player)
            "jump_boost" -> castJumpBoost(player)
            "speed" -> castSpeed(player)
            "levitation" -> castLevitation(player)
            "dash" -> castDashToTarget(player, target)
        }
    }

    // Target-specific spell effects
    private fun castFireballOnTarget(player: Player, target: org.bukkit.entity.Entity) {
        // Direct damage and fire effect
        if (target is org.bukkit.entity.LivingEntity) {
            target.damage(8.0, player)
            target.fireTicks = 60 // 3 seconds of fire
        }

        // Particle effects at target location
        player.world.spawnParticle(Particle.FLAME, target.location.add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5, 0.1)
        player.world.spawnParticle(Particle.LAVA, target.location.add(0.0, 1.0, 0.0), 10, 0.3, 0.3, 0.3, 0.1)
        player.world.playSound(target.location, Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.2f)
    }

    private fun castLightningBoltOnTarget(player: Player, target: org.bukkit.entity.Entity) {
        // Strike lightning at target location
        player.world.strikeLightning(target.location)

        // Additional electric damage
        if (target is org.bukkit.entity.LivingEntity) {
            target.damage(12.0, player)
        }

        player.world.spawnParticle(Particle.ELECTRIC_SPARK, target.location.add(0.0, 1.0, 0.0), 30, 0.5, 1.0, 0.5, 0.2)
    }

    private fun castIceShardOnTarget(player: Player, target: org.bukkit.entity.Entity) {
        // Ice damage and slow effect
        if (target is org.bukkit.entity.LivingEntity) {
            target.damage(6.0, player)
            target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 100, 1)) // 5 seconds slow
            target.addPotionEffect(PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 0)) // 5 seconds mining fatigue
        }

        // Ice particle effects
        player.world.spawnParticle(Particle.SNOWFLAKE, target.location.add(0.0, 1.0, 0.0), 25, 0.5, 0.5, 0.5, 0.1)
        player.world.spawnParticle(Particle.BLOCK, target.location.add(0.0, 1.0, 0.0), 15, 0.3, 0.3, 0.3, 0.1, Material.ICE.createBlockData())
        player.world.playSound(target.location, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f)
    }

    private fun castMagicMissileOnTarget(player: Player, target: org.bukkit.entity.Entity) {
        // Reliable magic damage
        if (target is org.bukkit.entity.LivingEntity) {
            target.damage(5.0, player)
        }

        // Magic particle effects
        player.world.spawnParticle(Particle.ENCHANT, target.location.add(0.0, 1.0, 0.0), 15, 0.5, 0.5, 0.5, 0.1)
        player.world.spawnParticle(Particle.WITCH, target.location.add(0.0, 1.0, 0.0), 10, 0.3, 0.3, 0.3, 0.1)
        player.world.playSound(target.location, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.8f)
    }

    private fun castTeleportToTarget(player: Player, target: org.bukkit.entity.Entity) {
        // Teleport behind the target
        val targetLocation = target.location.clone()
        val direction = targetLocation.direction.multiply(-2.0) // Behind the target
        val teleportLocation = targetLocation.add(direction)
        teleportLocation.yaw = player.location.yaw
        teleportLocation.pitch = player.location.pitch

        // Particle effects at both locations
        player.world.spawnParticle(Particle.PORTAL, player.location.add(0.0, 1.0, 0.0), 30, 0.5, 1.0, 0.5, 0.1)
        player.world.spawnParticle(Particle.PORTAL, teleportLocation, 30, 0.5, 1.0, 0.5, 0.1)

        player.teleport(teleportLocation)
        player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
        player.sendMessage("§5Teleported behind target!")
    }

    private fun castDashToTarget(player: Player, target: org.bukkit.entity.Entity) {
        // Dash towards the target
        val direction = target.location.subtract(player.location).toVector().normalize().multiply(2.0)
        direction.y = 0.3 // Add slight upward momentum
        player.velocity = direction

        player.world.spawnParticle(Particle.SWEEP_ATTACK, player.location, 5, 0.5, 0.1, 0.5, 0.1)
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f)
        player.sendMessage("§6Dash to target!")
    }

    // Helper function to round to nearest 0.5
    private fun roundToNearestHalf(value: Double): Double {
        return kotlin.math.round(value * 2.0) / 2.0
    }

    /**
     * Handle mana shield damage conversion
     */
    private fun handleManaShieldDamage(player: Player, damage: Double): Double {
        val currentTime = System.currentTimeMillis()
        val expirationTime = activeManaShields[player]
        
        // Check if shield is still active
        if (expirationTime == null || currentTime > expirationTime) {
            activeManaShields.remove(player)
            return damage // No shield active, return full damage
        }
        
        // Get player's RPG data
        val rpgPlayer = plugin.playerManager.getPlayer(player)
        val currentMana = rpgPlayer.mana
        
        // Calculate mana cost (2 mana per 1 damage)
        val manaCost = damage * 2.0
        
        if (currentMana >= manaCost) {
            // Player has enough mana - absorb all damage
            rpgPlayer.mana = (currentMana - manaCost).coerceAtLeast(0.0)
            
            // Visual feedback for successful absorption
            player.world.spawnParticle(Particle.ENCHANT, player.location.add(0.0, 1.0, 0.0), 8, 0.5, 0.8, 0.5, 0.1)
            player.world.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.8f)
            player.sendMessage("§b⚡ Mana Shield absorbed §c${String.format("%.1f", damage)} damage §b(§9-${String.format("%.1f", manaCost)} mana§b)")
            
            return 0.0 // No damage taken
        } else if (currentMana > 0) {
            // Player has some mana - absorb partial damage
            val absorbedDamage = currentMana / 2.0
            val remainingDamage = damage - absorbedDamage
            
            rpgPlayer.mana = 0.0
            
            // Visual feedback for partial absorption
            player.world.spawnParticle(Particle.ENCHANT, player.location.add(0.0, 1.0, 0.0), 5, 0.3, 0.5, 0.3, 0.1)
            player.world.spawnParticle(Particle.CRIT, player.location.add(0.0, 1.0, 0.0), 3, 0.3, 0.5, 0.3, 0.1)
            player.world.playSound(player.location, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.5f)
            player.sendMessage("§b⚡ Mana Shield partially absorbed §c${String.format("%.1f", absorbedDamage)} damage §7(§4Out of mana!§7)")
            
            // Deactivate shield since mana is depleted
            activeManaShields.remove(player)
            player.sendMessage("§7Your Mana Shield has been depleted.")
            
            return remainingDamage.coerceAtLeast(0.0)
        } else {
            // No mana left - shield fails
            activeManaShields.remove(player)
            player.sendMessage("§7Your Mana Shield failed due to insufficient mana.")
            player.world.spawnParticle(Particle.SMOKE, player.location.add(0.0, 1.0, 0.0), 8, 0.5, 0.5, 0.5, 0.05)
            
            return damage // Full damage
        }
    }
}
