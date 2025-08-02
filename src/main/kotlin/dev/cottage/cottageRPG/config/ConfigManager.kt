package dev.cottage.cottageRPG.config

import dev.cottage.cottageRPG.CottageRPG
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.util.logging.Level

/**
 * Manages plugin configuration with automatic reloading and validation
 */
class ConfigManager(private val plugin: CottageRPG) {
    
    private var config: FileConfiguration = plugin.config
    private val configFile = File(plugin.dataFolder, "config.yml")
    
    init {
        loadConfig()
    }
    
    /**
     * Load or reload the configuration file
     */
    fun loadConfig() {
        try {
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }
            
            if (!configFile.exists()) {
                plugin.saveDefaultConfig()
            }
            
            config = YamlConfiguration.loadConfiguration(configFile)
            validateConfig()
            
            if (getBoolean("general.debug")) {
                plugin.logger.info("Configuration loaded successfully")
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to load configuration", e)
        }
    }
    
    /**
     * Save the current configuration to file
     */
    fun saveConfig() {
        try {
            config.save(configFile)
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Failed to save configuration", e)
        }
    }
    
    /**
     * Reload the configuration from file
     */
    fun reloadConfig() {
        loadConfig()
    }
    
    /**
     * Validate configuration values and set defaults if needed
     */
    private fun validateConfig() {
        var changed = false
        
        // Validate general settings
        if (!config.contains("general.debug")) {
            config.set("general.debug", false)
            changed = true
        }
        
        if (!config.contains("general.prefix")) {
            config.set("general.prefix", "&7[&6CottageRPG&7]&r")
            changed = true
        }
        
        // Validate player settings
        if (!config.contains("player.starting_level")) {
            config.set("player.starting_level", 1)
            changed = true
        }
        
        if (!config.contains("player.max_level")) {
            config.set("player.max_level", 100)
            changed = true
        }
        
        // Validate database settings
        if (!config.contains("database.type")) {
            config.set("database.type", "sqlite")
            changed = true
        }
        
        if (changed) {
            saveConfig()
            plugin.logger.info("Configuration updated with missing values")
        }
    }
    
    // Convenience methods for getting configuration values
    
    fun getString(path: String): String? = config.getString(path)
    
    fun getString(path: String, default: String): String = config.getString(path, default) ?: default
    
    fun getInt(path: String): Int = config.getInt(path)
    
    fun getInt(path: String, default: Int): Int = config.getInt(path, default)
    
    fun getDouble(path: String): Double = config.getDouble(path)
    
    fun getDouble(path: String, default: Double): Double = config.getDouble(path, default)
    
    fun getBoolean(path: String): Boolean = config.getBoolean(path)
    
    fun getBoolean(path: String, default: Boolean): Boolean = config.getBoolean(path, default)
    
    fun getStringList(path: String): List<String> = config.getStringList(path)
    
    fun getLong(path: String): Long = config.getLong(path)
    
    fun getLong(path: String, default: Long): Long = config.getLong(path, default)
    
    fun getFloat(path: String): Float = config.getDouble(path).toFloat()
    
    fun getFloat(path: String, default: Float): Float = config.getDouble(path, default.toDouble()).toFloat()
    
    /**
     * Set a configuration value
     */
    fun set(path: String, value: Any?) {
        config.set(path, value)
    }
    
    /**
     * Check if a configuration path exists
     */
    fun contains(path: String): Boolean = config.contains(path)
    
    /**
     * Get the raw FileConfiguration object
     */
    fun getConfig(): FileConfiguration = config
}
