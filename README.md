# 🎲 The Game Chest

A modular, cross-platform Android digital board game hub featuring classic, racing, and family cooperative board games with fluid physics animations, dynamic rule mutators, custom dice loadouts, and local multiplayer.

---

## 📲 Download & Play

You can directly download the latest compiled Android APK from this repository:

👉 **[Download The Game Chest APK (apk/TheGameChest-debug.apk)](./apk/TheGameChest-debug.apk)**

---

## 🎮 Included Game Packages

### 1. 🏎️ Rev-Up Racers: Turbo Circuit
- **Genre**: Hybrid Racing / Snakes & Ladders
- **Board Layout**: 49 exact circuit spaces from **START (0)** to **FINISH (49)**.
- **Turbo Bridges**: 5 overpass shortcuts (`7 -> 29`, `11 -> 31`, `14 -> 35`, `18 -> 37`, `27 -> 45`).
- **Oil Spills**: 3 hazardous slides (`25 -> 4` / `26 -> 5`, `33 -> 12`, `41 -> 22`).
- **Game Mutators**:
  1. *Classic Grand Prix (1d6)*: Standard roll & sprint; roll a 6 to roll again!
  2. *Nitro Target (1d60)*: Direct target jump across the track; overshoot 49 to lose turn.
  3. *Nitro Assist (1d60 Forward-Only)*: Forward assist bounded by your current position.
  4. *Reverse Hazard Overdrive*: Slicks become speed boosts, ramps send you to pit lane.
  5. *Custom Grid & Dice*: Custom starting pole positions and custom dice (1d2 to 1d100).

---

### 2. 🐑 Save the Sheep! («Σώσε τα Προβατάκια»)
- **Genre**: Cooperative & Racing Family Board Game
- **Board Layout**: 40 meadow country tiles from the **Pasture Gate (Tile 0)** to the **Cozy Barn Home (Tile 40)**.
- **Safe Bridges**: 3 wooden stream crossings (`5 -> 18`, `20 -> 32`, `35 -> 39`).
- **Wolf Trails**: 2 sneaky wolf hazards (`14 -> 4`, `30 -> 16`).
- **Game Mutators**:
  1. *Co-op Meadow Rescue (1d6)*: Cooperate to guide all sheep to safety.
  2. *Wolf Alert Sprint (1d60)*: Jump to target meadow; roll > 40 alerts the wolf and forfeits your turn.
  3. *Sheepdog Escort (1d60 Assist)*: Guard dogs prevent backtracking.
  4. *Reverse Wolf Tracks*: Wolf tracks become sheepdog boosts.
  5. *Custom Shepherd Dice*: Assign custom dice per shepherd.

---

## ⚡ Core Features & Aesthetics

- **Fluid Car & Sheep Movement**: Dynamic piece steering, smooth ramp traversal, and orientation realignment.
- **Dynamic Player Queue**: Live player turn status in a sleek left sidebar with active glowing turns.
- **Scrollable Rolls Feed**: Color-coded roll history for each player with bonus refresh indicators (`↻`) and bridge (`↑`) / hazard (`↓`) arrows.
- **Quick Play Mode**: Persistent instant-advance mode with animated toast banners and auto-roll capability.
- **Security & Privacy**: Keystores, signing keys, and private credentials are excluded from the repository.

---

## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/enigma9q/The-game-chest.git
cd The-game-chest

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

---

## 📄 License
Open source under the [MIT License](LICENSE).
