package dev.cottage.cottageRPG.database

import dev.cottage.cottageRPG.CottageRPG
import dev.cottage.cottageRPG.classes.RPGClass
import dev.cottage.cottageRPG.player.RPGPlayer
import java.io.File
import java.sql.*
import java.util.*
import java.util.logging.Level

/**
 * Manages database operations for player data storage
 */
class DatabaseManager(private val plugin: CottageRPG) {
    
    private var connection: Connection? = null
    private val config = plugin.configManager
    
    init {
        initializeDatabase()
        createTables()
    }
    
    /**
     * Initialize database connection
     */
    private fun initializeDatabase() {
        try {
            val dbType = config.getString("database.type", "sqlite").lowercase()
            
            connection = when (dbType) {
                "sqlite" -> {
                    val dbFile = File(plugin.dataFolder, config.getString("database.sqlite_file", "cottageRPG.db"))
                    if (!plugin.dataFolder.exists()) {
                        plugin.dataFolder.mkdirs()
                    }
                    DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
                }
                "mysql" -> {
                    val host = config.getString("database.host", "localhost")
                    val port = config.getInt("database.port", 3306)
                    val database = config.getString("database.database", "cottageRPG")
                    val username = config.getString("database.username", "root")
                    val password = config.getString("database.password", "password")
                    
                    DriverManager.getConnection(
                        "jdbc:mysql://$host:$port/$database?useSSL=false&serverTimezone=UTC",
                        username,
                        password
                    )
                }
                "postgresql" -> {
                    val host = config.getString("database.host", "localhost")
                    val port = config.getInt("database.port", 5432)
                    val database = config.getString("database.database", "cottageRPG")
                    val username = config.getString("database.username", "root")
                    val password = config.getString("database.password", "password")
                    
                    DriverManager.getConnection(
                        "jdbc:postgresql://$host:$port/$database",
                        username,
                        password
                    )
                }
                else -> throw IllegalArgumentException("Unsupported database type: $dbType")
            }
            
            plugin.logger.info("Database connection established ($dbType)")
            
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize database", e)
        }
    }
    
    /**
     * Create necessary database tables
     */
    private fun createTables() {
        try {
            connection?.let { conn ->
                // Players table
                val createPlayersTable = """
                    CREATE TABLE IF NOT EXISTS rpg_players (
                        uuid VARCHAR(36) PRIMARY KEY,
                        level INTEGER NOT NULL DEFAULT 1,
                        experience BIGINT NOT NULL DEFAULT 0,
                        health DOUBLE NOT NULL DEFAULT 20.0,
                        max_health DOUBLE NOT NULL DEFAULT 20.0,
                        mana DOUBLE NOT NULL DEFAULT 100.0,
                        max_mana DOUBLE NOT NULL DEFAULT 100.0,
                        class_id VARCHAR(50),
                        money DOUBLE NOT NULL DEFAULT 0.0,
                        last_seen BIGINT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent()
                
                // Player skills table
                val createSkillsTable = """
                    CREATE TABLE IF NOT EXISTS rpg_player_skills (
                        uuid VARCHAR(36) NOT NULL,
                        skill_name VARCHAR(50) NOT NULL,
                        level INTEGER NOT NULL DEFAULT 0,
                        experience BIGINT NOT NULL DEFAULT 0,
                        PRIMARY KEY (uuid, skill_name),
                        FOREIGN KEY (uuid) REFERENCES rpg_players(uuid) ON DELETE CASCADE
                    )
                """.trimIndent()
                
                conn.createStatement().use { stmt ->
                    stmt.execute(createPlayersTable)
                    stmt.execute(createSkillsTable)
                }
                
                plugin.logger.info("Database tables created successfully")
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "Failed to create database tables", e)
        }
    }
    
    /**
     * Load player data from database
     */
    fun loadPlayer(uuid: UUID): RPGPlayer? {
        return try {
            connection?.let { conn ->
                val query = "SELECT * FROM rpg_players WHERE uuid = ?"
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()
                    
                    if (rs.next()) {
                        val player = RPGPlayer(
                            uuid = uuid,
                            level = rs.getInt("level"),
                            experience = rs.getLong("experience"),
                            health = rs.getDouble("health"),
                            maxHealth = rs.getDouble("max_health"),
                            mana = rs.getDouble("mana"),
                            maxMana = rs.getDouble("max_mana"),
                            money = rs.getDouble("money"),
                            lastSeen = rs.getLong("last_seen")
                        )
                        
                        // Load class if exists
                        val classId = rs.getString("class_id")
                        if (classId != null) {
                            player.rpgClass = RPGClass.getDefaultClasses().find { it.id == classId }
                        }
                        
                        // Load skills
                        loadPlayerSkills(uuid, player)
                        
                        player
                    } else {
                        null
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Failed to load player $uuid", e)
            null
        }
    }
    
    /**
     * Load player skills from database
     */
    private fun loadPlayerSkills(uuid: UUID, player: RPGPlayer) {
        try {
            connection?.let { conn ->
                val query = "SELECT * FROM rpg_player_skills WHERE uuid = ?"
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()
                    
                    while (rs.next()) {
                        val skillName = rs.getString("skill_name")
                        val level = rs.getInt("level")
                        val experience = rs.getLong("experience")
                        
                        player.skills[skillName] = level
                        player.skillExperience[skillName] = experience
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Failed to load skills for player $uuid", e)
        }
    }
    
    /**
     * Save player data to database
     */
    fun savePlayer(player: RPGPlayer) {
        try {
            connection?.let { conn ->
                // Save main player data
                val upsertPlayer = """
                    INSERT OR REPLACE INTO rpg_players 
                    (uuid, level, experience, health, max_health, mana, max_mana, class_id, money, last_seen)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                
                conn.prepareStatement(upsertPlayer).use { stmt ->
                    stmt.setString(1, player.uuid.toString())
                    stmt.setInt(2, player.level)
                    stmt.setLong(3, player.experience)
                    stmt.setDouble(4, player.health)
                    stmt.setDouble(5, player.maxHealth)
                    stmt.setDouble(6, player.mana)
                    stmt.setDouble(7, player.maxMana)
                    stmt.setString(8, player.rpgClass?.id)
                    stmt.setDouble(9, player.money)
                    stmt.setLong(10, player.lastSeen)
                    stmt.executeUpdate()
                }
                
                // Save skills
                savePlayerSkills(player)
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Failed to save player ${player.uuid}", e)
        }
    }
    
    /**
     * Save player skills to database
     */
    private fun savePlayerSkills(player: RPGPlayer) {
        try {
            connection?.let { conn ->
                // Delete existing skills
                val deleteSkills = "DELETE FROM rpg_player_skills WHERE uuid = ?"
                conn.prepareStatement(deleteSkills).use { stmt ->
                    stmt.setString(1, player.uuid.toString())
                    stmt.executeUpdate()
                }
                
                // Insert current skills
                val insertSkill = """
                    INSERT INTO rpg_player_skills (uuid, skill_name, level, experience)
                    VALUES (?, ?, ?, ?)
                """.trimIndent()
                
                conn.prepareStatement(insertSkill).use { stmt ->
                    for ((skillName, level) in player.skills) {
                        val experience = player.skillExperience[skillName] ?: 0L
                        stmt.setString(1, player.uuid.toString())
                        stmt.setString(2, skillName)
                        stmt.setInt(3, level)
                        stmt.setLong(4, experience)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Failed to save skills for player ${player.uuid}", e)
        }
    }
    
    /**
     * Get top players by level
     */
    fun getTopPlayersByLevel(limit: Int = 10): List<Pair<String, Int>> {
        val results = mutableListOf<Pair<String, Int>>()
        try {
            connection?.let { conn ->
                val query = "SELECT uuid, level FROM rpg_players ORDER BY level DESC, experience DESC LIMIT ?"
                conn.prepareStatement(query).use { stmt ->
                    stmt.setInt(1, limit)
                    val rs = stmt.executeQuery()
                    
                    while (rs.next()) {
                        val uuid = UUID.fromString(rs.getString("uuid"))
                        val level = rs.getInt("level")
                        val playerName = org.bukkit.Bukkit.getOfflinePlayer(uuid).name ?: "Unknown"
                        results.add(playerName to level)
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Failed to get top players", e)
        }
        return results
    }
    
    /**
     * Get a player's rank position
     */
    fun getPlayerRank(uuid: UUID): Int {
        try {
            connection?.let { conn ->
                val query = """
                    SELECT COUNT(*) + 1 as rank 
                    FROM rpg_players p1, rpg_players p2 
                    WHERE p1.uuid = ? 
                    AND (p2.level > p1.level OR (p2.level = p1.level AND p2.experience > p1.experience))
                """.trimIndent()
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        return rs.getInt("rank")
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Failed to get player rank for $uuid", e)
        }
        return -1 // Return -1 if player not found or error occurred
    }
    
    /**
     * Close database connection
     */
    fun close() {
        try {
            connection?.close()
            plugin.logger.info("Database connection closed")
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Error closing database connection", e)
        }
    }
}
