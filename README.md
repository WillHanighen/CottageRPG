# CottageRPG - Minecraft RPG Plugin

A comprehensive, maintainable, and configurable PaperMC plugin for Minecraft 1.21.5 that adds RPG features to your server.

## Features

### 🎯 Core RPG System
- **Player Levels & Experience**: Custom leveling system with configurable experience rates
- **Health & Mana System**: Extended health and mana pools that scale with level
- **Class System**: Choose from Warrior, Mage, Archer, or Rogue classes
- **Skills System**: 10 different skills including Mining, Combat, Magic, and more
- **Economy Integration**: Built-in money system with configurable starting amounts

### 🛡️ Classes
- **Warrior**: High health, melee combat specialist
- **Mage**: High mana, magical abilities
- **Archer**: Ranged combat expert
- **Rogue**: Stealth and agility focused

### ⚒️ Skills
- **Gathering**: Mining, Woodcutting, Farming, Fishing
- **Combat**: Combat, Archery
- **Magic**: Magic, Alchemy
- **Crafting**: Cooking, Smithing

### 🎮 User Interface
- **Interactive GUIs**: Beautiful inventory-based interfaces
- **Real-time Stats**: View player stats, skills, and progress
- **Class Selection**: Easy class choosing with requirement checking
- **Top Players**: Leaderboard system

### ⚙️ Configuration
- **Highly Configurable**: Extensive config.yml with 50+ options
- **Database Support**: SQLite, MySQL, and PostgreSQL support
- **World Management**: Enable/disable features per world
- **Performance Settings**: Async processing and caching options

## Installation

1. Download the latest release
2. Place the JAR file in your server's `plugins` folder
3. Start your server to generate the configuration files
4. Configure the plugin in `plugins/CottageRPG/config.yml`
5. Restart your server

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/rpg` | Main RPG command menu | `cottageRPG.use` |
| `/rpg stats` | View your character stats | `cottageRPG.use` |
| `/rpg skills` | View your skills | `cottageRPG.use` |
| `/rpg class` | Choose your class | `cottageRPG.use` |
| `/rpg top` | View top players | `cottageRPG.use` |
| `/rpg reload` | Reload configuration | `cottageRPG.admin.reload` |
| `/stats` | Quick access to stats GUI | `cottageRPG.use` |
| `/skills` | Quick access to skills GUI | `cottageRPG.use` |
| `/class [name]` | Select or view class | `cottageRPG.use` |

## Configuration

The plugin is highly configurable through `config.yml`. Key sections include:

### General Settings
```yaml
general:
  debug: false
  prefix: "&7[&6CottageRPG&7]&r"
  language: "en"
```

### Player System
```yaml
player:
  starting_level: 1
  starting_health: 20.0
  starting_mana: 100.0
  max_level: 100
  exp_multiplier: 1.0
```

### Database Configuration
```yaml
database:
  type: "sqlite"  # sqlite, mysql, postgresql
  sqlite_file: "cottageRPG.db"
  # MySQL/PostgreSQL settings available
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `cottageRPG.use` | Basic plugin usage | `true` |
| `cottageRPG.admin` | Admin permissions | `op` |
| `cottageRPG.admin.reload` | Reload configuration | `op` |

## API Usage

The plugin provides a comprehensive API for developers:

```kotlin
// Get RPG player data
val rpgPlayer = CottageRPG.instance.playerManager.getPlayer(player)

// Add experience
rpgPlayer.addExperience(100L)

// Add skill experience
rpgPlayer.addSkillExperience("mining", 50L)

// Set player class
val warriorClass = RPGClass.getDefaultClasses().find { it.id == "warrior" }
rpgPlayer.setClass(warriorClass!!)
```

## Building

This project uses Gradle with Kotlin DSL:

```bash
./gradlew build
```

The built JAR will be in `build/libs/`.

## Dependencies

- **PaperMC API**: 1.21.5-R0.1-SNAPSHOT
- **Kotlin**: 2.2.20-Beta2
- **Java**: 21+

## Database Schema

The plugin automatically creates the following tables:

- `rpg_players`: Main player data (level, experience, health, mana, class, money)
- `rpg_player_skills`: Player skill levels and experience

## Performance

- **Async Processing**: Heavy operations run asynchronously
- **Caching**: Player data is cached in memory
- **Periodic Saves**: Automatic data saving every 5 minutes
- **Connection Pooling**: Efficient database connections

## Compatibility

- **Minecraft Version**: 1.21.5
- **Server Software**: PaperMC (recommended), Spigot
- **Java Version**: 21+

### Plugin Compatibility
The plugin checks for and warns about potentially conflicting plugins:
- McMMO
- SkillAPI
- Heroes

## Support

For support, bug reports, or feature requests:
- Create an issue on GitHub
- Join our Discord server
- Visit our website: cottage-dev.net

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Changelog

### Version 1.0.0
- Initial release
- Core RPG system with levels, classes, and skills
- Interactive GUI system
- Database support (SQLite, MySQL, PostgreSQL)
- Comprehensive configuration system
- Performance optimizations

## Credits

- **Author**: Cottage Dev
- **Contributors**: [List contributors here]
- **Special Thanks**: PaperMC team for the excellent API

---

**Note**: This plugin is designed for RPG-themed servers and may conflict with other RPG plugins. Always test in a development environment first.
