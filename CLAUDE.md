# CLAUDE.md — CityWorld Compass

Nature's Compass for CityWorld: right-click, pick a place, the needle points to the nearest one. Mod id
`cityworld_compass`, **GPL-3.0-only** (it compiles against CityWorld's GPL-3 API: never relabel it). Built 2026-10-17
by the CityWorld session; `BRIEF.md` is the original design, `art/` the item art.

| | |
|---|---|
| Home branch | `mc26.2` (docs live here; features land here and are cherry-picked) |
| Dev server | port 25620 (`run-mc<version>/server.properties`, gitignored); Vivo claim `:2` / 25620 / RCON 25630 |
| JDKs | `tools` is a symlink to `../CityWorld-ReForged/tools` (jdk17 / jdk21 / jdk25) |

```bash
CW_RANGE='[6.5.0,)' scripts/selftest.sh jdk25     # fresh world, the COMPASS SELFTEST lines, PASS/FAIL; kills its own server
./gradlew build                                    # build/libs/cityworld_compass-<v>+mc<mc>.jar
```

`CW_RANGE` is only needed while the dev CityWorld jar still calls itself older than `cityworld_version_range`
(`[6.6.0,)`). **Run one self-test at a time**: two at once once crashed with "Address already in use".

## How it works

- **The search is CityWorld's**: `CityWorldAPI.findables(level)` (the menu: `Findable(id, group, title)`) and
  `CityWorldAPI.find(level, from, id, radius, 1, budgetMillis)` (`LotInfo`, nearest first). Plan only: nothing is
  generated. `place:vault` points at the hut door (`CityWorldAPI.vaultEntrance`); everything else at the chunk centre
  at street level. If the API needs to change, change it in CityWorld (`../CityWorld-ReForged`, `api/CityWorldAPI`).
- **Flow**: item `use` (server) sends `OpenMenu` with the findables; the client screen sends `Seek(id, hand)`;
  `Search.seek` runs `find` on a two-thread daemon pool, ticks "Seeking… Ns" every 2 s, then sets the target. A
  search that ran ≥95% of its budget and found nothing says "gave up" (a retry goes further: CityWorld keeps the plan).
- **The needle is vanilla's lodestone needle.** 26.x / 1.21.11: `minecraft:lodestone_tracker` (tracked=false) and an
  item model that `range_dispatch`es on `minecraft:compass` (target `lodestone`, fallback `none` = spinning).
  1.21.1: the same component, an `angle` property registered in client setup, and an `overrides` model. 1.20.1: the
  `LodestonePos`/`LodestoneDimension`/`LodestoneTracked` NBT tags. The 32 frames are vanilla's order (frame 0 points
  down, then clockwise); pivot (8, 9) because the skyline takes the top rows. Regenerate them from
  `art/urban_compass_16_skyline_b.png` if the art changes (the script is in the CityWorld session's history; it is a
  few lines of PIL).
- **Loot**: a global loot modifier (`CityWorldChestLoot`) adds the compass with `chance` (0.005) to any
  `cityworld:chests/*` table, never the `_extra` hooks. The self-test rolls `chests/building` 4,000 times (expect ~20).

## Versions

| Branch | Worktree | MC / loader / Java | What differs from `mc26.2` |
|---|---|---|---|
| `mc26.3` | `../CityWorld-Compass-worktrees/mc26.3` | 26.3 / NeoForge 26.3.0.58-beta / 25 | `LootModifier(Optional<Holder<LootItemCondition>>, priority)`; JSON has no `conditions` |
| `mc26.1` | `.../mc26.1` | 26.1.2 / 26.1.2.95 / 25 | `minecraft.setScreen` (26.2 moved it to `minecraft.gui`) |
| `main` | `.../mc1.21.11` | 1.21.11 / 21.11.42 / 21 | `displayClientMessage(c, true)`; `GuiGraphics` + `renderContent`/`drawString`; `LootModifier(conditions)`; needs `data/neoforge/loot_modifiers/global_loot_modifiers.json` (26.x loads every file in `loot_modifiers/` and must NOT have that list file) |
| `mc1.21.1` | `.../mc1.21.1` | 1.21.1 / 21.1.251 / 21 | `ResourceLocation`; `InteractionResultHolder`; list-tooltip `appendHoverText`; row `render(...)` entries, 250 ms double-click; `PacketDistributor.sendToServer`; `angle` item property + `overrides` model; recipe keys `{"item": ...}` |
| `mc1.20.1` | `.../mc1.20.1` | 1.20.1 / Forge 47.4.23 / 17 | Forge: `SimpleChannel`, `ForgeConfigSpec` (COMMON), `Supplier<Codec>` loot modifier, lodestone NBT, a hand-laid screen, `recipes/`, `data/forge/loot_modifiers/global_loot_modifiers.json`, a mandatory `pack.mcmeta`. Compiles and runs against CityWorld's **`build/devlibs`** jar; **ship `build/libs`** (reobfuscated). |

Each line's `cityworld_jar` in `gradle.properties` points at that CityWorld worktree's jar. All six self-tests
passed on 2026-10-17 against CityWorld dev builds carrying the 6.6.0 API.

## Releasing

Not released yet. It ships **after** CityWorld 6.6.0 (the API it needs). The owner is creating the CurseForge project
and GitHub repo. Then: `mod_version` on every branch, `cityworld_version_range=[6.6.0,)`, point `cityworld_jar` at
the 6.6.0 jars, self-test every line, build, tag, a release with the six jars. `CURSEFORGE.md` is the store page.
