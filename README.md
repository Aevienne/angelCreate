# angelCreate

A Minecraft 1.21.1 Paper plugin that enables **player-run companies** to design and patent **custom products** with modular effect modules, craft them via a workbench system, and sell finished goods.

---

## Requirements

| Requirement | Version |
|---|---|
| Minecraft Server | Paper 1.21.1 |
| Java | 21+ |
| Build Tool | Gradle 8.x |
| API Version | 1.21 |
| Vault | Any current release |

---

## Building

```bash
./gradlew jar
```

Output: `build/libs/angelCreate-1.0.0.jar`

Drop the jar into your server's `plugins/` folder and restart.

---

## Features

- **Company management** — Create and manage companies with role-based member tiers
- **Product design** — Create custom products with configurable base materials
- **Modular effects** — Add up to 5 effect modules per product (passive, on-hit, on-wear)
- **Workbench GUI** — Craft and finalize products through an inventory-based interface
- **Patent system** — Lock down product designs with time-limited patents
- **Patent expiry** — Automatic purge of expired patents every 5 minutes
- **Caravan integration** — Hook to angelTrade for product sales
- **YAML persistence** — Companies, products, and patents saved across restarts
- **Vault economy** — Cost-based company creation and patent registration

---

## Commands

### /company

| Command | Description |
|---|---|
| `/company create <name>` | Create a new company |
| `/company list` | List all companies on the server |
| `/company info [company]` | View company details and members |
| `/company join <company>` | Request to join a company |
| `/company leave` | Leave your current company |
| `/company invite <player>` | Invite a player (MANAGER+) |
| `/company kick <player>` | Remove a member (MANAGER+) |
| `/company promote <player> <role>` | Change member role (OWNER) |
| `/company disband` | Disband your company (OWNER only) |

### /product

| Command | Description |
|---|---|
| `/product create <name>` | Create a new product (MANAGER+) |
| `/product list` | List your company's products |
| `/product info <product>` | View product details and effects |
| `/product setbase <product> <material>` | Set the base item (MANAGER+) |
| `/product addeffect <product> <effect>` | Add an effect module (MANAGER+) |
| `/product removeeffect <product> <slot>` | Remove an effect (MANAGER+) |
| `/product finalize <product>` | Lock the product design (MANAGER+) |
| `/product workbench <product>` | Open the crafting workbench |

### /patent

| Command | Description |
|---|---|
| `/patent create <product> <days>` | Patent a product design (MANAGER+) |
| `/patent list` | List your company's active patents |
| `/patent info <patent>` | View patent details and expiry |
| `/patent check <product>` | Check if a product is patented |

### /ceadmin

| Command | Description |
|---|---|
| `/ceadmin reload` | Reload all configs and data |
| `/ceadmin purgepatents` | Force purge all expired patents |

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `createecon.company.create` | true | Create companies |
| `createecon.company.list` | true | List companies |
| `createecon.company.info` | true | View company info |
| `createecon.company.join` | true | Join companies |
| `createecon.company.leave` | true | Leave company |
| `createecon.company.invite` | false | Invite players (MANAGER+) |
| `createecon.company.kick` | false | Kick members (MANAGER+) |
| `createecon.company.promote` | false | Promote members (OWNER) |
| `createecon.company.disband` | false | Disband company (OWNER) |
| `createecon.product.create` | false | Create products (MANAGER+) |
| `createecon.product.list` | true | List products |
| `createecon.product.info` | true | View product info |
| `createecon.product.setbase` | false | Set base material (MANAGER+) |
| `createecon.product.addeffect` | false | Add effects (MANAGER+) |
| `createecon.product.removeeffect` | false | Remove effects (MANAGER+) |
| `createecon.product.finalize` | false | Finalize product (MANAGER+) |
| `createecon.product.workbench` | true | Use workbench |
| `createecon.patent.create` | false | Create patents (MANAGER+) |
| `createecon.patent.list` | true | List patents |
| `createecon.patent.info` | true | View patent info |
| `createecon.patent.check` | true | Check patents |
| `createecon.admin` | op | Admin commands |

---

## How It Works

### 1. Company Creation

Create a company with `/company create <name>`. This deducts a creation cost from your balance (via Vault). You become the OWNER and can now create products and manage members.

**Company Roles:**
- **OWNER** — Full control; can disband company and promote members
- **MANAGER** — Create/edit products, invite members, patent designs
- **MEMBER** — View company info and use workbenches

### 2. Product Design

Create a product with `/product create <name>`, then configure it:

1. Set a base material with `/product setbase <product> <material>`
2. Add effect modules with `/product addeffect <product> <effect>` (up to 5)
3. Finalize the design with `/product finalize <product>`

Each effect has a Type (PASSIVE, ON_HIT, ON_WEAR) and a Trigger that determines when it activates.

### 3. Workbench Crafting

Open `/product workbench <product>` to access the crafting interface. Combine ingredients to craft the product and receive a finished custom item with stored effects.

### 4. Patent Protection

Patent a product design with `/patent create <product> <days>` to lock it down and prevent other companies from copying it. Patents automatically expire and are purged every 5 minutes.

Patenting costs economy funds — use this to establish market exclusivity.

### 5. Caravan Sales

Finished products are sold through **angelTrade's Trade Shops**. Link a shop to a trade route and list your products at your set prices.

---

## Configuration

**`plugins/angelCreate/config.yml`**

```yaml
economy:
  company-create-cost: 1000.0
  product-create-cost: 100.0
  patent-create-cost-per-day: 10.0

product-limits:
  max-effects: 5

patent:
  default-duration-days: 30
  purge-interval-ticks: 6000

messages:
  prefix: "&6[angelCreate] &r"
  insufficient-funds: "&cYou do not have enough funds."
  company-create-success: "&aCompany created: &6{company}"
  product-created: "&aProduct created: &6{product}"
```

**`plugins/angelCreate/effects.yml`**

Define custom effect types and their behavior:

```yaml
effects:
  MobAura:
    type: PASSIVE
    trigger: ALWAYS
    color: FF5500
    description: "Damages nearby mobs"
  
  LifeSteal:
    type: ON_HIT
    trigger: MELEE_HIT
    color: AA0000
    description: "Heal on successful hit"
```

---

## Data Storage

YAML files stored in `plugins/angelCreate/data/`:

| File | Purpose |
|---|---|
| `companies.yml` | Company data and member rosters |
| `products.yml` | Product definitions and effects |
| `patents.yml` | Active patents with expiry dates |

---

## Project Structure

```
src/main/java/me/angelique/angelCreate/
├── AngelCreate.java                    # Main plugin class
├── commands/
│   ├── CompanyCommand.java            # /company
│   ├── ProductCommand.java            # /product
│   ├── PatentCommand.java             # /patent
│   └── AdminCommand.java              # /ceadmin
├── gui/
│   ├── WorkbenchGUI.java              # Crafting interface
│   └── ProductSelectGUI.java          # Product selection
├── listeners/
│   ├── WorkbenchListener.java         # Workbench interactions
│   ├── ItemInteractListener.java      # Product item usage
│   └── EffectTriggerListener.java     # Effect activation
├── managers/
│   ├── CompanyManager.java            # Company CRUD
│   ├── ProductManager.java            # Product lifecycle
│   ├── PatentManager.java             # Patent handling
│   └── EffectManager.java             # Effect registration
├── models/
│   ├── Company.java
│   ├── Product.java
│   ├── Patent.java
│   ├── EffectModule.java
│   └── enums/
│       ├── Role.java
│       ├── EffectType.java
│       └── Trigger.java
└── hooks/
    └── CaravanHook.java               # angelTrade integration
```

---

## Main Class

```
me.angelique.angelCreate.AngelCreate
```
