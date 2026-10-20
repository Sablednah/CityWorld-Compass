# Changelog

## Unreleased

- **A search lets go of you as soon as it finishes**, and when you log out or the server stops, rather than through
  a task on the server thread. Hardening from an audit for state that survives into the next world: in testing the
  old way never got stuck (the stopped server still ran the task), but nothing now depends on that.

## 1.0.0

First release, on Minecraft 1.20.1 (Forge), 1.21.1, 1.21.11, 26.1, 26.2 and 26.3 (NeoForge). Needs CityWorld 6.6.0+.

- **The Urban Compass.** Use it to open a searchable list of everything CityWorld can find in this world: the
  nearest city, 33 kinds of place, 14 shop trades, 11 kinds of district and every named building. Pick one and the
  needle points to the nearest; the action bar says how far. Sneak-use searches again from where you stand.
- **Searches read CityWorld's city plan**, off the server thread, so nothing is generated and the game does not pause.
  A long search shows its progress, and one that runs out of time says so (searching again goes further). Server
  config: `searchRadius` (3000 blocks) and `searchSeconds` (60).
- **Crafted** from a compass, four bricks and four lodestones; **found** in about one CityWorld chest in 200 (a loot
  modifier over CityWorld's chest tables that leaves their `_extra` pack hooks alone).
