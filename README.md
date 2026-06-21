## Commands

### Administrative Commands

- `/hivemind`
- `/hivemind on`
- `/hivemind off`

Used to control HiveMind activity in worlds where the event is not permanently enabled.

---

## Activation

HiveMind can activate in two ways.

### Player attacks a mob

When a player damages a valid hostile mob:

```text
Player attacks mob
↓
HiveMind activates
↓
Nearby mobs join
```

### Mob targets a player

When a hostile mob acquires a player target:

```text
Mob notices player
↓
HiveMind activates
↓
Nearby mobs join
```

This allows combat encounters to escalate naturally even when the player did not start the fight.

---

## Dynamic Difficulty Scaling

HiveMind scales according to player progression.

### Early Game

0–1 Iron Armor Pieces

```text
Radius: 32 blocks
Max Joined Mobs: 10
```

### Mid Game

2–4 Iron Armor Pieces

```text
Radius: 48 blocks
Max Joined Mobs: 25
```

### Diamond Progression

1–3 Diamond Armor Pieces

```text
Radius: 64 blocks
Max Joined Mobs: 40
```

### Late Game

Full Diamond or Any Netherite Piece

```text
Radius: 128 blocks
Max Joined Mobs: 80
```

### MundoZ

Inside MundoZ:

```text
Radius: 128 blocks
Max Joined Mobs: 80
```

Always active regardless of equipment.

---

## Mob Progression

Different hostile mobs become available as player equipment improves.

### Base Tier

Always available:

- Zombies
- Zombie variants
- Basic hostile mobs

### Full Diamond Tier

Unlocks:

- Skeleton
- Stray
- Bogged
- Spider

### Netherite Tier

Unlocks:

- Creeper

### Full Netherite Tier

Unlocks:

- Witch
- Wither Skeleton

This creates increasingly dangerous encounters as players become stronger.

---

## Tactical Roles

HiveMind assigns tactical roles to participating mobs.

### Default

Standard frontline attackers.

### Backline

Assigned to:

- Skeletons

Purpose:

- Ranged support
- Pressure from distance

### Breacher

Assigned to:

- Creepers

Purpose:

- Defensive line disruption
- Area denial

### Pressure Units

Randomly assigned to some mobs.

Purpose:

- Maintain aggression
- Prevent easy disengagement

---

## Swarm Propagation

When a valid mob enters HiveMind:

```text
Mob joins swarm
↓
Player becomes target
↓
Nearby mobs are searched
↓
Valid mobs are recruited
↓
Roles are assigned
↓
Swarm expands
```

The propagation system is what creates the signature HiveMind combat experience.

---

## Action Bar

During active HiveMind periods, players receive a timer display.

Example:

```text
Sobreviva: 04:59
```

The countdown shows how much active event time remains.

---

## Rewards

Mobs that die while actively participating in HiveMind may drop rewards.

Current reward:

```text
33% chance
↓
1 Emerald
```

This provides an incentive to engage with the event rather than avoid it.

---

## Skeleton Horseman Event

During thunderstorms, HiveMind may create special encounters.

### Overworld

Lower spawn chance.

### MundoZ

Much higher spawn chance.

Spawn composition:

```text
Skeleton Horse
+
Armed Skeleton Rider
+
Lightning Strike
```

Characteristics:

- Spawns near players
- Immediately hostile
- Designed as a surprise elite encounter

---

## Excluded Entities

Some entities are intentionally excluded from HiveMind behavior.

Examples include:

- Enderman
- Zombified Piglin
- Warden
- Wither
- Ender Dragon

These entities use special vanilla mechanics that do not fit HiveMind propagation.

---

## Technical Details

- Minecraft version: 26.2
- Fabric Loader: 0.19.3+
- Java: 25
- Environment: Server Only
- Uses Fabric Events
- Uses Mixins

### Main Components

#### Core

- HiveMindMod

#### Mixins

- MobSetTargetMixin

#### Interfaces

- HiveMindFlag
- SwarmRoleFlag

---

## Design Philosophy

HiveMind was designed around a simple idea:

Players become stronger over time.

Hostile mobs should evolve too.

Instead of increasing health values or damage numbers, HiveMind increases coordination.

The goal is to create more interesting combat rather than simply larger numbers.

---

## Relationship with MundoZ

HiveMind and MundoZ are independent mods but are designed to work together.

HiveMind provides:

- Combat behavior
- Difficulty scaling
- Swarm mechanics

MundoZ provides:

- Dedicated environment
- Permanent activation
- Safe death handling
- PvE progression space

Together they form the core PvE gameplay experience of the MundoZ server.

---

## Future Improvements

Possible future improvements:

- Additional tactical roles
- More elite encounter types
- Boss integration
- Better event notifications
- More reward varieties
- Configurable progression rules
- Additional weather-based encounters
- Improved balancing

---

## License

All Rights Reserved.
