# CottageRPG - Minecraft RPG Plugin

A comprehensive, maintainable, and configurable PaperMC plugin for Minecraft 1.21.5 that adds RPG features to your server.

## Features

### 🎯 Core RPG System
- **Player Levels & Experience**: Custom leveling system with configurable experience rates
- **Health & Mana System**: Extended health and mana pools that scale with level
- **Class System**: Choose from Warrior, Mage, Archer, or Rogue classes
- **Skills System**: 10 different skills including Mining, Combat, Magic, and more
- **Economy Integration**: Built-in money system with configurable starting amounts

### 🔮 Magic & Spell System
- **Spell Casting**: Comprehensive magic system with multiple spell types
  - **Fireball**: Offensive projectile spell that launches fireballs at enemies
  - **Dash**: Quick movement ability for rapid positioning
  - **Shield**: Defensive spell providing temporary protection
  - **Heal**: Self-healing ability to restore health
  - **Teleport**: Advanced movement spell for instant transportation
- **Spell Categories**: 
  - **Primary**: Main offensive spells for combat
  - **Secondary**: Defensive and support spells
  - **Special/Movement**: Utility and movement abilities
- **Spell Requirements**: Each spell has magic level requirements, mana costs, and cooldowns
- **Interactive Spell GUI**: Beautiful inventory-based spell selection interface
- **Target-based Casting**: Some spells can target specific entities for enhanced combat

### 🪄 Magic Wand Crafting
- **Custom Crafting Recipes**: Create magic wands using configurable recipes
- **Material Customization**: Customize crafting materials (default: Lapis Lazuli + Stick)
- **Recipe Patterns**: Fully configurable crafting patterns via config.yml
- **Magic Wand Integration**: Wands serve as spell casting tools for the magic system

### 📊 Advanced Scoreboard System
- **Real-time RPG Display**: Shows comprehensive player information including:
  - Player class and level
  - Current money/economy status
  - Health and mana with visual progress bars
  - Active spell cooldowns
  - Experience progress
- **Visual Progress Bars**: Beautiful ASCII-style bars for health/mana visualization
- **Event-driven Updates**: Automatically updates on:
  - Experience gain
  - Health/mana changes
  - Money transactions
  - Class changes
  - Skill progression
- **Player Control**: Toggle scoreboard on/off per player preference
- **Configurable Display**: Customize title, colors, and update frequency

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
| `/rpg spells` | Open spell selection GUI | `cottageRPG.use` |
| `/rpg scoreboard` | Toggle scoreboard display | `cottageRPG.use` |
| `/rpg reload` | Reload configuration | `cottageRPG.admin.reload` |
| `/stats` | Quick access to stats GUI | `cottageRPG.use` |
| `/skills` | Quick access to skills GUI | `cottageRPG.use` |
| `/class [name]` | Select or view class | `cottageRPG.use` |
| `/spells` | Quick access to spells GUI | `cottageRPG.use` |

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

### Spell System Configuration
```yaml
spells:
  enabled: true
  cooldown_reduction_per_level: 0.1
  mana_cost_reduction_per_level: 0.05
  
  # Individual spell settings
  fireball:
    enabled: true
    base_damage: 6.0
    mana_cost: 20.0
    cooldown: 5
    required_magic_level: 1
    
  dash:
    enabled: true
    distance: 5.0
    mana_cost: 15.0
    cooldown: 3
    required_magic_level: 5
    
  heal:
    enabled: true
    heal_amount: 4.0
    mana_cost: 25.0
    cooldown: 8
    required_magic_level: 3
```

### Magic Wand Crafting Configuration
```yaml
crafting:
  wand:
    enabled: true
    pattern:
      - "aba"
      - "aba" 
      - "aba"
    materials:
      a: "LAPIS_LAZULI"
      b: "STICK"
```

### Scoreboard Configuration
```yaml
scoreboard:
  enabled: true
  title: "§6§lCottage RPG"
  update_interval: 20  # ticks (1 second)
  show_health_bar: true
  show_mana_bar: true
  show_cooldowns: true
  bar_length: 10
  filled_char: "█"
  empty_char: "░"
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `cottageRPG.use` | Basic plugin usage | `true` |
| `cottageRPG.admin` | Admin permissions | `op` |
| `cottageRPG.admin.reload` | Reload configuration | `op` |

## Magic System Usage

### Spell Casting
1. Craft or obtain a magic wand using the crafting recipe
2. Hold the magic wand and right-click to open the spell selection GUI
3. Choose your desired spell from the available categories
4. Left-click to cast spells directly, or right-click on entities to target them
5. Monitor your mana and spell cooldowns via the scoreboard

### Spell Progression
- Spells require specific magic skill levels to unlock
- Higher magic levels reduce mana costs and cooldowns
- Each spell has unique effects and tactical applications
- Combine spells strategically for effective combat and utility

### Available Spells

#### Primary Spells (Offensive)
- **Fireball**: Launches a projectile that explodes on impact, dealing area damage
  - Unlocked at Magic Level 1
  - Base damage: 6.0 hearts
  - Mana cost: 20

#### Secondary Spells (Defensive/Support)
- **Shield**: Provides temporary damage resistance and protection effects
  - Defensive buff spell
  - Mana cost varies by duration
- **Heal**: Instantly restores health to the caster
  - Unlocked at Magic Level 3
  - Heal amount: 4.0 hearts
  - Mana cost: 25

#### Special/Movement Spells
- **Dash**: Rapidly propels the player forward in their facing direction
  - Unlocked at Magic Level 5
  - Distance: 5 blocks
  - Mana cost: 15
- **Teleport**: Instantly transports the player to their target location
  - Advanced movement spell
  - Can target specific locations or entities
  - Higher mana cost for longer distances

### Magic Wand Crafting Guide
1. Gather materials (default: 6x Lapis Lazuli + 3x Stick)
2. Arrange in crafting table using the configured pattern:
   ```
   L S L
   L S L  
   L S L
   ```
   (L = Lapis Lazuli, S = Stick)
3. The crafted wand will have special properties for spell casting
4. Right-click with the wand to access the spell selection interface

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

// Spell system integration
val spellManager = CottageRPG.instance.spellManager
val playerSpells = spellManager.getPlayerSpells(player)

// Scoreboard management
val scoreboardManager = CottageRPG.instance.scoreboardManager
scoreboardManager.toggleScoreboard(player)
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
