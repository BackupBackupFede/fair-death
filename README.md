# Fair Death

**Choose what you keep when you die.**

By default, your gear stays on you. The rest drops.
Operators change that in a chest menu.
Nothing to install on the client.

Fabric and NeoForge, Minecraft 1.21.1, 26.2 and 26.3. No dependencies.

## Five switches

`/fairdeath settings` opens a chest. Green: kept. Red: drops. Click to switch.

| Switch | Default | Covers |
|---|---|---|
| Armour | kept | armour pieces, elytra |
| Tools & weapons | kept | everything else with durability |
| Food & Drinks | drops | food, potions |
| Ores | drops | ores, ingots, gems, netherite, storage blocks |
| Utilities | drops | buckets, throwables, arrows, fireworks |

Kept items go back to the slot they came from.

## Fixed rules

- Always kept: totem, spyglass, recovery compass, filled maps, signed books.
- Always drops: shulker boxes, bundles, anything that holds items.
- Curse of Vanishing still works.
- Experience drops as in vanilla.

## Find the rest

- Dropped items glow.
- Your death coordinates, in chat, for you only.

## Commands

| Command | |
|---|---|
| `/fairdeath check` | is the item in your hand kept, and why |
| `/fairdeath settings` | the switches (operators) |
| `/fairdeath reload` | reload the config (operators) |

## Config and datapacks

`config/fairdeath.json` holds the five switches under `kept`, plus `glowingDrops` and
`deathCoordinates`.

Item tags add items to a list: `fairdeath:always_keep`, `fairdeath:always_drop`, `fairdeath:ores`,
`fairdeath:utilities`.

## Good to know

- With `keepInventory` on, Fair Death does nothing.
- Log out on the death screen: your kept items are still there.
- Nothing is stored outside the player's own save.

## Building

The Gradle process needs JDK 25, even for the 1.21.1 line (its toolchain, Java 21, is downloaded
automatically).

```bash
./gradlew build                             # default line
./gradlew build -Pminecraft_version=1.21.1  # any line of MATRIX in build.gradle
```

Jars land in `fabric/build/libs/` and `neoforge/build/libs/`.

## License

MIT
