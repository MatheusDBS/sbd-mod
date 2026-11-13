# Skyblock Dailies

A client-side Fabric mod for **Hypixel SkyBlock** (Minecraft 26.2) that keeps track of your daily tasks and adds a few money-making helpers.

> Not affiliated with or endorsed by Hypixel.

## Features

### Daily checklist — press `K` or type `/dailies`
- Tasks grouped by island: General, Private Island, Hub, Garden, Dwarven Mines, Crimson Isle, Dungeons, Galatea, Rift and Timers.
- Click a task to tick it. Hover it to see where to go, what to do, the reward and when it resets (in your local time).
- **Per-task resets**: most dailies reset at midnight New York (Hypixel server time), Bachelor's Roses at midnight UTC, the Matriarch at 8 PM New York, and cooldown tasks (Garden, Experimentation, Rift, Timers) a set time after you tick them.
- **Auto-tick from chat**: Fetchur, Heavy Pearls, Experimentation Table, Rosewater Flask, Visitors and Free Trial tick themselves; Faction Quests (5), Mini-bosses (5), Dungeon runs (5), Starlyn Contests (3) and Tree Gifts (250) count up.
- **Timers**: 13 cooldowns (Time Tower, Booster Cookie, Century Cakes, Aranya, ...). When any cooldown ends you get a "Ready again" notice.
- **Edit mode**: hide tasks you don't do, and add your own tasks with a name, section and reset (daily, every 1h–2 days, weekly).
- **Join reminder**: 15 seconds after joining SkyBlock, a title tells you how many dailies are left.

### Island HUD
A small list of the pending dailies for the island you are on. Use **HUD ✎** in the menu to drag it, resize it with the mouse wheel, change its background or turn it off.

### Item tooltips
Hover a SkyBlock item to see:
- **Forge**: forge time, materials with prices, cost, sell value, profit and **profit per forge hour**.
- **Craft**: cost to craft vs price to buy, and which is cheaper.
- **Sell**: NPC price vs market price.
- **Used in**: how many recipes use the item and the most profitable one.
- **Minion**: base output per day and coins per day (no fuel/upgrades).

### Commands
| Command | What it does |
|---|---|
| `/dailies` | Opens the checklist (same as `K`). |
| `/bestcraft <material>` | Top 5 most profitable crafts using a material (Tab completes). With no argument it uses the item in your hand. |
| `/bestforge` | Top 10 Forge items by profit per forge hour. |
| `/gabagool <amount>` | Very Crude → Hypergolic Gabagool calculator. |

## Requirements
- Minecraft **26.2**, Fabric Loader **0.20.1+**, **Fabric API**, Java 25.
- Recipes are read from the NotEnoughUpdates repo that **SkyHanni** or **Skyblocker** already download (`skyhanni/neurepo` or `config/skyblocker/item-repo`). Without one of them, `/bestcraft`, `/bestforge` and the price tooltips have no recipes.

## Data sources
- Bazaar prices: Hypixel public API `/v2/skyblock/bazaar` (no API key), cached for 5 minutes.
- NPC prices: Hypixel public API `/v2/resources/skyblock/items`, once per session.
- Lowest BIN: [Coflnet](https://sky.coflnet.com).
- Recipes: [NotEnoughUpdates-REPO](https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO).
- Chat patterns and reset times were cross-checked against SkyHanni, Skyblocker and the [Hypixel SkyBlock Wiki](https://hypixelskyblock.minecraft.wiki/w/Tutorial:Periodic_Tasks). Hypixel can change any message at any time.

## Hypixel rules
The mod only reads what the game already shows you (chat, scoreboard, tab list, item data) and public price APIs. It never sends commands, clicks, or automates gameplay, and its commands run only on your client. As with any mod, use it at your own risk ([Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962)).

## Saved data
In the profile's `config` folder:
- `skyblockdailies.txt` — ticked tasks.
- `skyblockdailies-prefs.txt` — hidden tasks, HUD settings, counters and custom tasks.

## Building
```bash
./gradlew build
```
Needs a Java 25 runtime to run Gradle (the JDK itself is downloaded by the Gradle toolchain). The jar ends up in `build/libs/`. Tests: `./gradlew test`.

### Editing the task list
`src/client/java/dev/skyblockdailies/Tasks.java` is generated. Edit the lists in `tools/make_sheet.py` (Portuguese source, also builds the spreadsheet) and `tools/en_tasks.py` (English text), then:
```bash
cd tools
python gen_tasks.py ../src/client/java/dev/skyblockdailies/Tasks.java
```
The Portuguese task name is the stable task id, so saved progress survives text changes.

## License
CC0-1.0 (see `LICENSE`).
