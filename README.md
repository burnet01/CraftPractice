# CraftPractice

A sophisticated competitive PvP practice plugin for Minecraft servers, designed to provide a complete dueling and practice environment with advanced features like custom knockback, hit delay management, tournaments, and comprehensive statistics tracking.

## 🎯 Features

### Core Gameplay
- **1v1 Duel System** - Fast-paced competitive matches with proper matchmaking
- **Ranked & Unranked Queues** - Separate queues for casual and competitive play
- **Kit-Based Combat** - Multiple combat styles with custom loadouts
- **Arena Management** - Dynamic arena allocation with terrain regeneration
- **Spectator Support** - Watch ongoing matches with proper visibility management

### Special Game Modes
- **Sumo Mode** - No items, fist-fighting only with arena-based combat
- **Boxing Mode** - Speed effects and hit counting system
- **Build Mode** - Block placement/destruction with arena regeneration
- **Tournament System** - Hosted tournaments with bracket progression

### Advanced Customization
- **Custom Knockback** - Fine-tuned knockback profiles with detailed control
- **Hit Delay Management** - Custom hit delay profiles for different playstyles
- **Per-Kit Statistics** - Separate ELO, wins, and losses for each kit
- **Player Settings** - GUI-based settings with real-time application

### Performance & Compatibility
- **Folia Support** - Full compatibility with Folia servers
- **MongoDB Integration** - Persistent player data storage
- **Optimized Scoreboards** - State-based scoreboards with minimal performance impact
- **Concurrent Operations** - Thread-safe design for high-traffic servers

## 🚀 Quick Start

### Installation
1. Download the latest CraftPractice.jar from the releases page
2. Place the jar in your server's `plugins` folder
3. Start/restart your server
4. Configure MongoDB connection in `plugins/CraftPractice/config.yml`
5. Set up arenas using `/arena create` command

### Basic Setup
```bash
# Create your first arena
/arena create MyArena
/arena setspawn1 MyArena
/arena setspawn2 MyArena
/arena setbounds MyArena

# Create a kit from your inventory
/kit create NoDebuff

# Join the queue
/queue join NoDebuff
```

## 📋 Commands

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/practice` | Main practice command with GUI | `practice.user` |
| `/queue join <kit>` | Join queue for specific kit | `practice.user` |
| `/queue leave` | Leave current queue | `practice.user` |
| `/spec <player>` | Spectate a player's match | `practice.user` |
| `/settings` | Open settings GUI | `practice.user` |
| `/spawn` | Teleport to practice spawn | `practice.user` |
| `/leaderboard` | View global leaderboards | `practice.user` |
| `/tournament` | Tournament management | `practice.user` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/arena` | Arena management | `practice.admin` |
| `/kit` | Kit management | `practice.admin` |
| `/kb` | Knockback management | `practice.admin` |
| `/hd` | Hit delay management | `practice.admin` |
| `/build` | Build mode bypass | `practice.build.bypass` |
| `/test` | Debug commands | `practice.admin` |

## ⚙️ Configuration

### config.yml
```yaml
# MongoDB Configuration
mongodb:
  uri: "mongodb://localhost:27017"
  database: "practice"

# Match Settings
match:
  countdown: 5
  max-duration: 600

# Queue Settings
queue:
  elo-k-factor: 32

# Arena Settings
arena:
  auto-regenerate: true

# Kit Settings
kit:
  default-kits:
    - "nodebuff"
    - "builduhc"
    - "sumo"
```

### Plugin.yml
```yaml
name: CraftPractice
main: rip.thecraft.practice.Practice
version: 0.0.1
description: Competitive PvP Practice Plugin
author: TheCraft
folia-supported: true
```

## 🏗️ System Architecture

### Core Systems
- **Match Manager** - Handles match lifecycle and player states
- **Queue Manager** - Manages ranked/unranked queues with ELO-based matchmaking
- **Kit Manager** - Kit creation, editing, and special mode handling
- **Arena Manager** - Arena allocation, terrain regeneration, and bounds management
- **Player Manager** - Player data, statistics, and state management

### Advanced Systems
- **Tournament System** - Single-elimination brackets with prize support
- **Knockback System** - Custom knockback profiles with fine-tuned control
- **Hit Delay System** - Custom hit delay profiles for different playstyles
- **Scoreboard Service** - State-based scoreboards with Folia optimization
- **Settings System** - GUI-based player preferences

## 🎮 Game Modes

### Standard Duel
- 1v1 combat with custom kits
- Arena-based combat with proper boundaries
- Automatic match start with countdown
- Win by eliminating opponent

### Sumo Mode
- No items, fist-fighting only
- Arena-based combat with knock-out win condition
- Simple and fast-paced gameplay

### Boxing Mode
- Speed potion effects applied automatically
- Hit counting system (first to 5 hits wins)
- Enhanced movement mechanics

### Build Mode
- Block placement and destruction enabled
- Arena terrain regeneration after matches
- Special build arenas with proper bounds
- Item-based combat with building mechanics

## 📊 Statistics & ELO System

### Per-Kit Statistics
- Separate ELO rating for each kit
- Individual win/loss tracking
- Weighted global ELO calculation
- Persistent MongoDB storage

### ELO Calculation
- K-factor: 32 (configurable)
- Weighted average based on games played
- Legacy global ELO for compatibility
- Real-time updates during matches

## 🏆 Tournament System

### Features
- **Player-Hosted Tournaments** - Any player can host tournaments
- **Single Elimination** - Standard bracket progression
- **Prize Commands** - Configurable rewards for winners
- **Spectator Support** - Watch tournament matches
- **Auto-Progression** - Automatic match advancement

### Tournament Commands
```bash
# Host a tournament
/tournament host NoDebuff 16

# Join a tournament
/tournament join

# View tournament info
/tournament info
```

## 🔧 Advanced Features

### Knockback Customization
Create custom knockback profiles with detailed control:
- Horizontal/Vertical knockback values
- Extra knockback settings
- Friction coefficients
- Sprint reset mechanics
- Netherite knockback resistance

### Hit Delay Management
Fine-tune hit delay for different playstyles:
- Custom hit delay profiles
- Real-time application
- Admin management tools

### Scoreboard Optimization
- State-based display (lobby, queue, match, spectating)
- Folia-compatible implementation
- Cached updates for performance
- Player-toggleable display

## 🗄️ Database Integration

### MongoDB Collections
- **player_data** - Player statistics and kit data
- Persistent storage with BSON serialization
- Legacy data migration support
- Concurrent access with proper locking

### Data Structure
```json
{
  "_id": "player-uuid",
  "playerName": "PlayerName",
  "globalElo": 1500,
  "globalWins": 50,
  "globalLosses": 25,
  "kitStats": {
    "nodebuff": {
      "elo": 1600,
      "wins": 30,
      "losses": 10
    }
  }
}
```

## 🚀 Performance Optimizations

### Folia Compatibility
- Region-scheduled tasks
- Location-based execution
- Thread-safe data structures
- Minimal main thread usage

### Memory Management
- Efficient arena terrain compression
- Concurrent data structures
- Proper resource cleanup
- Minimal object creation

### Scoreboard Optimization
- Cached scoreboard updates
- State-based adapters
- Minimal API calls
- Performance metrics tracking

## 🔒 Permissions

### Default Permissions
- `practice.user` - Basic user commands (default)
- `practice.admin` - Full administrative access
- `practice.arena` - Arena management
- `practice.kit` - Kit management
- `practice.build.bypass` - Build mode bypass

### Permission Hierarchy
```
practice.*
├── practice.admin
│   ├── practice.arena
│   ├── practice.kit
│   └── practice.build.bypass
└── practice.user
```

## Building

**Requirements**: 
  1. Maven
  2. JDK 17+ (Higher is better (21-24), current Compiler target is 17 though, may change in the future)
  3. Preferably using an IDE (IntelliJ IDEA is what I use)
```bash
# Clean install
mvn clean install
```

## 🐛 Troubleshooting

### Common Issues
1. **Arena not regenerating** - Check arena bounds and build mode settings
2. **Queue not matching** - Verify kit availability and arena allocation
3. **Scoreboard not updating** - Check player settings and Folia compatibility
4. **Database connection** - Verify MongoDB URI and network connectivity

### Debug Commands
```bash
# Check player state
/test check <player>

# Force end match
/test forceend <player>

# Debug information
/test debug
```

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:
1. Fork the repository
2. Create a feature branch
3. Follow existing code style
4. Add tests for new features
5. Submit a pull request

### Code Standards
- Use Java 17 features where appropriate
- Follow Minecraft plugin best practices
- Include proper documentation
- Ensure Folia compatibility

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Built for the Minecraft competitive community
- Inspired by popular practice server implementations
- Thanks to all contributors and testers

---

**CraftPractice** - Professional-grade PvP practice for competitive Minecraft servers
