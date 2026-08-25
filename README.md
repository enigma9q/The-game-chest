# 🎲 The Game Chest (v0.0.12)

A modular, cross-platform (Windows PC & Android) digital board game hub featuring classic, racing, and family cooperative board games with fluid continuous road physics, dynamic rule mutators, custom dice loadouts, and **Local Wi-Fi Co-op Multiplayer**.

---

## 🌐 Local Wi-Fi Co-Op Multiplayer (Cross-Platform PC & Android)

Play together seamlessly across any Windows PC and Android phones/tablets on the same local Wi-Fi:
- **Host Room (Server)**: Generates and displays the local Wi-Fi IP address (e.g. `192.168.1.xxx:8998`), tracks connected peers, and enables **START RACE** once all players are Ready.
- **Join Room (Client)**: Enter the Host IP, pick your racer & vehicle model, and click **READY FOR RACE**.
- **Synchronized Race Engine**: Live synchronized dice rolls, player moves, turn banners, and extra-roll rewards.
- **Pass & Play (Same Device)**: Single-device local multiplayer remains available with a single tap.

---

## 🏎️ Vehicle Lineup (8 North-Facing High-Res Models)

1. **Red GT** - High-speed sport hatchback
2. **Blue Speedster** - Agile track racer
3. **Gold Classic** - Classic roadster convertible
4. **Green Muscle** - Heavy V8 muscle car
5. **Police 911** - Interceptor patrol vehicle
6. **Velocity GT** - Ultra-aerodynamic supercar with rear wing
7. **Trash Truck** - Heavy-duty municipal sanitation truck
8. **Fire Truck** - Emergency ladder rescue engine

---

## 🎮 Included Game Packages

### 1. 🏎️ Rev-Up Racers: Turbo Circuit
- **Genre**: Hybrid Racing / Snakes & Ladders
- **Board Layout**: 49 calibrated circuit spaces from **START (0)** to **FINISH (49)**.
- **Turbo Bridges**: 5 overpass shortcuts (`7 -> 29`, `11 -> 31`, `14 -> 35`, `18 -> 37`, `27 -> 45`).
- **Oil Spills**: 4 hazard entry slides (`25 -> 4`, `26 -> 5`, `33 -> 12`, `41 -> 22`) with 360° spin-out animations.
- **Game Mutators**:
  1. *Classic Grand Prix (1d6)*: Standard roll & sprint; roll a 6 to roll again!
  2. *Nitro Target (1d60)*: Direct target jump; landing on an occupied tile triggers `"Roll again! Space occupied by other player"`; in-place turn-around & 2-way bridge navigation; roll > 49 forfeits turn.
  3. *Nitro Assist (1d60 Forward-Only)*: Forward assist bounded by current position.
  4. *Reverse Hazard Overdrive*: Slicks become speed boosts, ramps overheat.
  5. *Custom Grid & Dice*: Custom pole positions and dice loadouts (1d2 to 1d100).

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

- **Continuous Road Driving Physics**: High-FPS continuous spline trajectory motion with tail-out corner drifts and counter-steer recovery.
- **Turn Prompt Banner**: Electric blue banner (`It is your turn to play! / Roll the dice.`) guiding active racers.
- **Dynamic Player Queue**: Live player turn status in a sleek left sidebar with active glowing turns and distinct badge colors (`P1`, `P2`...).
- **Scrollable Rolls Feed**: Color-coded roll history for each player with bonus refresh indicators (`↻`) and bridge (`↑`) / hazard (`↓`) arrows.
- **Quick Play Mode**: Instant-advance auto-flow mode with lightning icon toggle.

---

## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/enigma9q/The-game-chest.git
cd The-game-chest

# Build Android APK
./gradlew :app:assembleDebug

# Build Windows Desktop Executable
./gradlew :desktopApp:createDistributable
```

---

## 📄 License
Open source under the [MIT License](LICENSE).
