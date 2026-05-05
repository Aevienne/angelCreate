# angelTrade

A Paper 1.21.1 Minecraft plugin implementing two interconnected economy systems —
**Trade Routes** and **Trade Shops (Caravans)** — designed to complement a supply/demand
shop economy and company system.

---

## Requirements

| Requirement | Version |
|---|---|
| Minecraft Server | Paper 1.21.1 |
| Java | 21+ |
| Build Tool | Gradle 8.x |
| API Version | 1.21 |
| Vault | Any current release |
| angelTrade | Optional (soft-depend) |

---

## Building

```bash
./gradlew jar
```

Output: `build/libs/angelTrade-1.0.0.jar`

Drop the jar into your server's `plugins/` folder and restart.

---

## Commands

### /route

| Subcommand | Description | Permission |
|---|---|---|
| `create` | Begin route creation (hold a Route Deed, right-click two Waystones) | `angeltrade.route.create` |
| `list` | List your trade routes | `angeltrade.route.list` |
| `info <id>` | View route details and current tier | `angeltrade.route.info` |
| `insure <id>` | Insure a route against decay (costs from personal balance) | `angeltrade.route.insure` |
| `remove <id>` | Remove a route you own | `angeltrade.route.remove` |

### /tradeshop

| Subcommand | Description | Permission |
|---|---|---|
| `place` | Receive a Trade Shop block | `angeltrade.tradeshop.use` |
| `relocate <id>` | Relocate your shop (cooldown applies) | `angeltrade.tradeshop.use` |
| `additem <id> <material> <price>` | Add an item listing to your shop | `angeltrade.tradeshop.use` |
| `removeitem <id> <material>` | Remove an item listing | `angeltrade.tradeshop.use` |
| `setprice <id> <material> <price>` | Update an item's price | `angeltrade.tradeshop.use` |
| `info <id>` | View shop details and linked route | `angeltrade.tradeshop.use` |
| `give_waystone` | Give yourself a Waystone *(admin only)* | `angeltrade.admin` |
| `give_deed` | Give yourself a Route Deed *(admin only)* | `angeltrade.admin` |

All permissions default to `true` (all players). `angeltrade.admin` defaults to `op`.

---

## How It Works

### 1. Trade Routes

```
Craft a Waystone → place at Location A
    └── Hold a Route Deed → right-click Waystone A
        └── Right-click Waystone B
            └── Route registered if within max distance (default 1000 blocks)
```

Routes gain tiers as they are used:

| Tier | Uses | Value Bonus |
|---|---|---|
| Dirt Road | 0–20 | +5% item value |
| Stone Road | 21–60 | +12% item value |
| Gold Road | 61–150 | +20% + passive income |
| Royal Road | 151+ | +30% + passive income |

### 2. Route Decay

Routes that go unused degrade automatically via a daily background task:

| Days Unused | Result |
|---|---|
| 7 | Warning notification sent to owner |
| 14 | INACTIVE — no bonuses applied |
| 21 | BROKEN — route deleted |

Broken routes have a **3-day grace period** — replace the Waystone at the same location
to fully restore the route and its tier progress.

Insured routes pay out from their insurance pool to the owner on sabotage or inactivity.

### 3. Sabotage

If a player who is **not** the route owner destroys a Waystone:
- The saboteur is logged in the database and flagged with a visible **Saboteur** tag
- The owner is notified with the saboteur's name
- Insured routes receive a partial payout automatically

### 4. Trade Shops

```
/tradeshop place → receive a Trade Shop (Chest) block
    └── Place it in the marketplace
        └── Right-click to open the shop GUI
            └── Players see company price vs server price side by side
```

Shops linked to an active route receive a **passive income bonus** on every sale.
Higher route tier = higher bonus percentage.

### 5. Crafting Recipes

All custom items are craftable in a standard crafting table:

**Waystone**
```
G G G
G L G    G = Gold Ingot, L = Lodestone
G G G
```

**Route Deed**
```
_ F _
_ P _    F = Feather, P = Paper, I = Ink Sac
_ I _
```

**Trade Shop Block**
```
_ G _
E C E    G = Gold Block, E = Emerald, C = Chest
_ E _
```

---

## Configuration

**`plugins/angelTrade/config.yml`**

```yaml
max-route-distance: 1000

decay:
  warn-days: 7
  inactive-days: 14
  broken-days: 21

revival-grace-days: 3

tiers:
  dirt-road-max: 20
  stone-road-max: 60
  gold-road-max: 150

upkeep:
  dirt-road: 5.0
  stone-road: 10.0
  gold-road: 20.0
  royal-road: 40.0

insurance-pool-fraction: 0.25

tradeshop-relocate-cooldown: 300

route-bonus:
  dirt-road: 0.05
  stone-road: 0.12
  gold-road: 0.20
  royal-road: 0.30

route-passive-income:
  dirt-road: 0.02
  stone-road: 0.05
  gold-road: 0.10
  royal-road: 0.15

currency-symbol: '$'
```

All `&` color codes are supported in the `messages:` section.

---

## ⚠️ WIP Integration Notices

### angelEconomy (Server /shop price display)
The Trade Shop GUI is designed to show the server `/shop` price alongside company prices
for direct comparison. This is currently stubbed in `TradeShopGUI.java`:

```java
// Stub: replace with actual server shop price lookup
lore.add("§7Server Price:  §f$?.?? §8(angelEconomy — WIP)");
```

Replace with your angelEconomy price-lookup API call when available.

### angelCompany (Company ownership & ledger)
Routes and Shops store `companyId` as a plain String. Once angelCompany is available,
replace with proper company API calls. Search for `// STUB: angelCompany` in the source
to find all integration points.

---

## Data Storage

SQLite — stored at `plugins/angelTrade/angeltrade.db` with WAL mode enabled.

| Table | Purpose |
|---|---|
| `trade_routes` | All route data including tier, status, insurance |
| `trade_shops` | All shop data including location and linked route |
| `shop_items` | Per-shop item listings with price, stock, discount |
| `saboteurs` | Sabotage log per player UUID with offense count |

---

## Project Structure

```
src/main/java/me/angelique/angelTrade/
├── AngelTrade.java                      # Main plugin class
├── commands/
│   ├── RouteCommand.java                # /route
│   └── TradeShopCommand.java           # /tradeshop
├── gui/
│   └── TradeShopGUI.java               # Chest GUI with company + server price
├── listeners/
│   ├── WaystoneListener.java           # Waystone place/break + Route Deed flow
│   └── TradeShopListener.java          # Shop right-click, block-break protection
├── managers/
│   ├── BonusManager.java               # Route value bonus + passive income
│   ├── RecipeManager.java              # Crafting recipe registration
│   ├── RouteManager.java               # Route lifecycle, decay, saboteur handling
│   └── TradeShopManager.java          # Shop CRUD, relocate, item management
├── models/
│   ├── ShopItem.java
│   ├── TradeRoute.java
│   └── TradeShop.java
└── data/
    └── DataManager.java                # SQLite with WAL, auto-reconnect
```

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `angeltrade.route.create` | true | Create trade routes |
| `angeltrade.route.list` | true | List your routes |
| `angeltrade.route.info` | true | View route info |
| `angeltrade.route.insure` | true | Insure routes |
| `angeltrade.route.remove` | true | Remove routes |
| `angeltrade.tradeshop.use` | true | Use trade shop commands |
| `angeltrade.admin` | op | Admin override for all actions |

---

## Main Class

```
me.angelique.angelTrade.AngelTrade
```
