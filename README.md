<div align="center">

<img src="src/main/resources/assets/bbetter_seasons/better_seasons_logo.png" alt="Betty's Better Seasons" width="220" />

# Betty's Better Seasons

**A data-driven seasons & calendar library for Minecraft Forge 1.20.1**

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen)
![Forge](https://img.shields.io/badge/Forge-47.4.23+-orange)
![Java](https://img.shields.io/badge/Java-17-blue)
![License](https://img.shields.io/badge/License-LGPL--3.0-lightgrey)

</div>

---

Betty's Better Seasons is a **library mod**. It tracks the date, decides which crops are in season, and
fires events — then gets out of the way. Consumer mods decide what seasons, festivals, and crop rules
actually *mean* for their content. With no data loaded, it changes nothing; remove every consumer mod and
it is still a useful calendar for a vanilla pack.

> **Design rule:** the library owns date math, seasons, the calendar GUI, the HUD, and the seasonal crop
> gate. It never owns what a festival does, what a crop sells for, or which crops belong to which season —
> that is all opt-in through datapacks and the public API.

## ✨ Features

- **🗓️ Configurable calendar** — seasons, sub-seasons (Early/Mid/Late), days per season, week length,
  start year and ticks-per-day are all configurable. The date is derived from the world's day time, so
  sleeping, `/time add`, and lag-skips stay correct automatically.
- **🌱 Data-driven seasonal crops** — assign any block, block tag, or item to seasons via datapack JSON.
  Per-crop out-of-season rules (`none` / `slow` / `stop` / `wither`), per-season growth multipliers, and
  peak sub-season bonuses. Ships **no** crop data of its own, so it is inert until a pack adds some.
- **🥀 Lazy withering & greenhouses** — out-of-season crops wither on their next tick; two-block crops can
  handle their own removal via `ISeasonalCrop`. Exempt crops with a greenhouse soil tag, a dimension list,
  or a custom `SeasonExemption`.
- **📅 Calendar events** — birthdays, festivals, holidays and more, created in-game, from datapack
  templates, or in code. Recurring or one-time, with permission-gated editing.
- **📋 Calendar event presets** — save a set of events as a preset (in-game, no code) and pick it on the
  *Create New World* screen to start a new world with its calendar already filled in.
- **🖥️ UI** — a month-grid calendar screen with event-type icons (also an **embeddable widget** for your
  own screens) that pauses singleplayer while open, a date HUD, seasonal "Grows in" tooltips, chat
  notifications, and a season-transition fade.
- **🔌 Clean public API** — `BetterSeasonsAPI` (both sides) and `BetterSeasonsClientAPI`, plus Forge events
  for day/season/year/sub-season changes, event triggers, and crop checks.

## 📦 Requirements & versions

| | |
| --- | --- |
| Minecraft | `1.20.1` |
| Forge | `47.4.23+` (range `[47,)`) |
| Java | `17` |
| Mod ID / namespace | `bbetter_seasons` |
| Current version | `1.1.0-beta-1.20.1` |
| License | GNU LGPL 3.0 |

No other mod is required to run the library by itself.

## 🧑‍🤝‍🧑 For players

1. Install **Minecraft Forge 47.4.23+** for 1.20.1.
2. Drop `bbetter_seasons-<version>.jar` into your `mods/` folder.
3. If a mod lists Betty's Better Seasons as a dependency, both jars go in `mods/` together.

**In-game quick start:**

- Craft a **Calendar** — 8 paper around a clock. Right-click it to open the calendar;
  **Shift + right-click** a surface to place a **Wall Calendar**.
- Starting a new world? *Create New World → Game* tab → **Calendar Preset** to begin with your saved events
  already on the calendar.

See the [Players Guide](documentation/Players.md) for everything else.

## 🛠️ For mod developers

### 1. Add the dependency to `build.gradle`

> Once the library is published to CurseForge/Modrinth you can pull it from a Maven; until then, drop the
> jar into a local `libs/` folder and use the file dependency.

```gradle
repositories {
    // Option A — CurseMaven (after the project is published on CurseForge)
    maven { url = "https://cursemaven.com" }
    // Option B — a local folder for the jar
    flatDir { dirs "libs" }
}

dependencies {
    // Option A — CurseMaven (replace FILE_ID with the uploaded file id)
    // implementation fg.deobf("curse.maven:bettys-better-seasons-PROJECT_ID:FILE_ID")

    // Option B — local jar in libs/
    implementation fg.deobf("net.bettytiger:bbetter_seasons:1.1.0-beta-1.20.1")
}
```

### 2. Declare it in `META-INF/mods.toml`

```toml
[[dependencies."yourmodid"]]
    modId = "bbetter_seasons"
    mandatory = true                  # or false if you keep a no-seasons fallback
    versionRange = "[1.1.0-beta-1.20.1,)"  # match the version string you build against
    ordering = "AFTER"
    side = "BOTH"
```

### 3. Use the API

```java
import net.bettytiger.bbetter_seasons.api.BetterSeasonsAPI;
import net.bettytiger.bbetter_seasons.event.SeasonChangeEvent;

// Is this crop in season here (honors greenhouses, dimensions, exemptions)?
boolean ok = BetterSeasonsAPI.isInSeason(level, pos, state);

// Read the date (safe on client and server).
var date = BetterSeasonsAPI.getDate(level);          // CalendarDate: day, season, year, weekday…
var label = BetterSeasonsAPI.formatShort(level, date); // "Spring 9, Year 1"

// Speed a crop up during its peak (opt-in): multiply your growth chance by this.
float mod = BetterSeasonsAPI.getGrowthModifier(level, pos, state);

// Greenhouse-style rule.
BetterSeasonsAPI.registerExemption((lvl, p, st) -> hasGlassRoofAbove(lvl, p));

// React to the season turning.
@SubscribeEvent
public void onSeason(SeasonChangeEvent e) { /* e.getOldSeason(), e.getNewSeason() */ }
```

Crop blocks can also implement `ISeasonalCrop` to define default seasons/rules in code (datapacks always
win). Forge events live in `net.bettytiger.bbetter_seasons.event` — `DayChangeEvent`,
`SubSeasonChangeEvent`, `SeasonChangeEvent`, `YearChangeEvent`, `CalendarEventTriggerEvent`,
`CropSeasonCheckEvent`, `CropWitherEvent`, `SeasonalDataReloadEvent`, and `RegisterEventTypesEvent`.

## 🧩 Datapack extension points

All under `data/<your_namespace>/…` — the library scans every namespace.

| Path | Purpose |
| --- | --- |
| `seasons/*.json` | Define/override seasons: `name`, `color` (`"#RRGGBB"`), `order`, optional `icon` |
| `seasonal/crops/*.json` | Map crops to seasons with rules/growth/peak (see below) |
| `event_templates/*.json` | Pre-fill a world with a group of events (applied once per world) |

Seasonal crop entry example:

```json
{
  "targets": ["minecraft:wheat", "#forge:crops"],
  "seasons": ["bbetter_seasons:summer"],
  "out_of_season": "wither",
  "growth": { "bbetter_seasons:summer": 1.0 },
  "peak": { "season": "bbetter_seasons:summer", "sub_season": "mid", "growth": 1.25 }
}
```

Tag `bbetter_seasons:ignores_seasons` (block tag) marks soils that keep crops in season (greenhouses).

## ⚙️ Configuration

Forge TOML, generated on first run:

- **Client** (global): `config/bbetter_seasons-client.toml` — HUD on/off/corner/scale,
  seasonal tooltips, transition screen on/off + duration.
- **Server** (per-world): `<world>/serverconfig/bbetter_seasons-server.toml` — days per season, ticks per
  day, start year, sub-season splits, seasonal dimensions, seasonal-crop master switch, `slowFactor`,
  `witherDropsSeed`, default rule, chat notifications + lead days, and event-edit permission.

To change defaults for **new** worlds, place a server config in `defaultconfigs/`.

## ⌨️ Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/calendar` | all | Show the current date |
| `/calendar set <year> <season> <day>` | op (2) | Set the full date |
| `/calendar set year <year>` | op (2) | Set just the year |
| `/calendar set day <day>` | op (2) | Set just the day |
| `/season set <season>` | op (2) | Change just the season |
| `/season check [<pos>]` | all | Report a crop's seasonal status (look target or coords) |

## 📄 License & credits

Licensed under **GNU LGPL 3.0**. Created by **BettyTiger**.
