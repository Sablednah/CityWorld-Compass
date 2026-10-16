# CityWorld Compass — brief

A companion mod to CityWorld, in the spirit of Nature's Compass: right-click, pick a kind of place, and the needle
points to the nearest one. The owner agreed the shape on 2026-10-16. This file is the starting point for whoever
builds it (a fresh Claude session, like the CityWorld-Zoo one).

## What it finds

Everything is answered from CityWorld's **plan**, not the world. The plan is a pure function of the seed, so a search
generates nothing and works over land nobody has visited.

- **Places** (lot kinds): hospital, mall, museum, subway station, radio tower, zoo, police station, fire station,
  vault entrance (in a ruined world), and so on.
- **Named buildings**: placed schematics by name (Winchester, Fire Station, ...).
- **Districts**: downtown, industrial, harbour, park, ...
- **The nearest city** (vanilla-land worlds have cities far apart).

## Mechanics

1. **Right-click** opens a selection screen. It lists what this world can contain, grouped as above, with a search box.
2. **Pick an entry.** The server searches outward from the player within a radius (config) and a time budget.
   Searching the plan is cheap, but keep it off the server tick or bounded.
3. **The needle points at the result.** Store the target as a vanilla **lodestone tracker** on the stack
   (`minecraft:lodestone_tracker` with `tracked: false` from 1.20.5 on; `LodestonePos`/`LodestoneTracked` NBT on 1.20.1).
   Vanilla's compass model then points there, so the mod needs no custom renderer. Show the distance and the
   target's name on the tooltip, or on the action bar while held.
4. **Different dimensions:** the ruined Nether twin has the same plan, so the compass can work there too. Out of
   range or none found: say so and leave the needle spinning.

## How players get it — very rare loot AND craftable (owner, 2026-10-16)

- **Loot:** a NeoForge global loot modifier (`neoforge:loot_table_id` conditions over CityWorld's chest tables, e.g.
  `cityworld:chests/*`) plus `minecraft:random_chance` around 0.5–1%. Do NOT take over CityWorld's `_extra` hook
  tables: a pack owns those, and two owners conflict. 1.20.1 Forge uses `forge:loot_table_id`.
- **Recipe (owner's pick):** a compass in the middle, bricks on the four edges, **lodestones** in the four corners.
  Since 1.21.5 a lodestone is chiselled stone bricks around an iron ingot (it took netherite before), so on the
  26.x lines the recipe is mid-game, not late-game. Ship it as a datapack recipe, so packs can change it.

## Built against CityWorld's public API

`me.daddychurchill.CityWorld.api.CityWorldAPI`:
- `findLot`/`findLots(level, from, kind, maxBlocks, limit[, budgetMillis])` — nearest lots whose class name contains
  `kind`. `LotInfo` gives the chunk, lot class, district (`contextClass`), schematic name, shop type and interior name.
- `findVaultEntrances`, `vaultEntrance`, `lotAt`.

**Added for the compass (CityWorld 6.6.0, 2026-10-16):**
- `findables(level)` → `List<Findable(id, group, title)>`: the screen's menu. Groups are `city`, `place`, `shop`,
  `district` and `building`. Ids look like `city`, `place:hospital`, `shop:bookshop`, `district:industrial` and
  `building:winchester`. Building titles come from each schematic's `Title`.
- `find(level, from, id, maxBlocks, limit, budgetMillis)` → `List<LotInfo>`, nearest first. A multi-chunk place
  answers with its nearest chunk, and `place:vault` answers with the entrance hut only.
- `findLots(level, from, Predicate<LotInfo>, maxBlocks, limit, budgetMillis)`: search by anything a lot says.
- `nearestCity(level, from, maxBlocks)` → `Optional<BlockPos>`, the middle at street level. In vanilla-land worlds
  this is the city site; on CityWorld's own land it is the nearest built-up district.
- **Call these off the server thread**, with a budget, as `/cityfind` does; a search can plan many platmaps. The
  self-test (`checkFindables`) proves `place:subway`, `district:highrise` and `city` on every line.

Declare CityWorld as a required dependency, `[6.6.0,)`.

## Shape of the project

- **Mod id `cityworld_compass`, GPL-3.0-only.** It builds against CityWorld's GPL-3 API.
- **Start on 26.2 only**, where the owner plays (ZARP, LegendQuest). Port to the other lines once it feels right,
  using the same branch-per-version worktrees as CityWorld and CityWorld-Zoo.
- **Client code (screen, packets) differs on every Minecraft line.** Test the screen on a real client (WSLg
  `runClient`, or the Vivo box), not just a headless server: a dedicated server never loads client code.
- **Item art (chosen 2026-10-16):** `art/urban_compass_16_skyline_b.png`: the owner's 16×16 rim and face with a
  stepped skyline on top. The 32 needle frames are in `art/frames/urban_compass_00..31.png`, in vanilla's order:
  frame 0 points down, then clockwise (8 left, 16 up, 24 right). The needle pivots on the dial's centre pixel (8, 9),
  lower than vanilla's (8, 7) because the skyline takes the top rows. The needle is red, with a dark grey tail.
  Wire the frames like vanilla's `assets/minecraft/items/compass.json`: a `range_dispatch` on the `minecraft:compass`
  property with target `lodestone`, and a spinning fallback when the stack has no target.
