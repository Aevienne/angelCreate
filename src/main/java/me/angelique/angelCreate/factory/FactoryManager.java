package me.angelique.angelCreate.factory;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelNCore.events.EventBus;
import me.angelique.angelNCore.events.FactoryDamagedEvent;
import me.angelique.angelNCore.events.FactoryRepairedEvent;
import me.angelique.angelNCore.events.ItemProducedEvent;
import me.angelique.angelNCore.services.MarketService;
import me.angelique.angelNCore.services.ServiceRegistry;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class FactoryManager {

    private final AngelCreate plugin;
    private final Map<String, FactoryRecipe> recipes = new LinkedHashMap<>();
    private final Map<Long, FactoryBlock> factories = new ConcurrentHashMap<>();
    private long nextId = 1;
    private File factoriesFile;

    public FactoryManager(AngelCreate plugin) {
        this.plugin = plugin;
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    public void loadRecipes() {
        File file = new File(plugin.getDataFolder(), "factories.yml");
        if (!file.exists()) plugin.saveResource("factories.yml", false);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        recipes.clear();

        ConfigurationSection section = yaml.getConfigurationSection("recipes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                String base = "recipes." + key + ".";
                String displayName = yaml.getString(base + "display-name", key);
                org.bukkit.Material blockType = org.bukkit.Material.matchMaterial(
                        yaml.getString(base + "block-type", "FURNACE"));
                if (blockType == null) blockType = org.bukkit.Material.FURNACE;
                int tier = yaml.getInt(base + "tier", 1);
                int duration = yaml.getInt(base + "duration-seconds", 120);
                org.bukkit.Material fuelMat = org.bukkit.Material.matchMaterial(
                        yaml.getString(base + "fuel-material", "COAL"));
                if (fuelMat == null) fuelMat = org.bukkit.Material.COAL;
                int fuelAmt = yaml.getInt(base + "fuel-amount", 1);

                List<FactoryRecipe.RecipeStack> inputs = loadStacks(yaml, base + "inputs");
                List<FactoryRecipe.RecipeStack> outputs = loadStacks(yaml, base + "outputs");

                recipes.put(key, new FactoryRecipe(key, displayName, blockType, tier,
                        duration, fuelMat, fuelAmt, inputs, outputs));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load recipe: " + key, e);
            }
        }
        plugin.getLogger().info("Loaded " + recipes.size() + " factory recipes.");
    }

    private List<FactoryRecipe.RecipeStack> loadStacks(YamlConfiguration yaml, String path) {
        List<FactoryRecipe.RecipeStack> list = new ArrayList<>();
        List<Map<?, ?>> raw = yaml.getMapList(path);
        for (Map<?, ?> map : raw) {
            Object matObj = map.get("material");
            Object amtObj = map.get("amount");
            String matName = matObj != null ? String.valueOf(matObj).toUpperCase() : "AIR";
            int amount = amtObj != null ? Integer.parseInt(String.valueOf(amtObj)) : 1;
            org.bukkit.Material mat = org.bukkit.Material.matchMaterial(matName);
            if (mat != null) list.add(new FactoryRecipe.RecipeStack(mat, amount));
        }
        return list;
    }

    public void loadFactories() {
        factoriesFile = new File(plugin.getDataFolder(), "data/factories.yml");
        if (!factoriesFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(factoriesFile);
        factories.clear();
        nextId = 1;

        ConfigurationSection section = yaml.getConfigurationSection("factories");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                long id = Long.parseLong(key);
                String base = "factories." + key + ".";
                String world = yaml.getString(base + "world");
                double x = yaml.getDouble(base + "x");
                double y = yaml.getDouble(base + "y");
                double z = yaml.getDouble(base + "z");
                org.bukkit.World w = plugin.getServer().getWorld(world);
                if (w == null) continue;
                Location loc = new Location(w, x, y, z);
                UUID owner = UUID.fromString(yaml.getString(base + "owner"));
                String companyId = yaml.getString(base + "company-id", "");
                String recipeId = yaml.getString(base + "recipe-id");

                FactoryBlock block = new FactoryBlock(id, loc, owner, companyId, recipeId);
                block.setHealth(yaml.getDouble(base + "health", 100.0));
                block.setFuelBuffer(yaml.getInt(base + "fuel-buffer", 0));
                block.setStatus(FactoryBlock.Status.valueOf(
                        yaml.getString(base + "status", "IDLE")));
                block.setProductionStartMillis(yaml.getLong(base + "production-start", 0));

                ConfigurationSection inputs = yaml.getConfigurationSection(base + "input-storage");
                if (inputs != null) {
                    for (String k : inputs.getKeys(false)) block.getInputStorage().put(k, inputs.getInt(k));
                }
                ConfigurationSection outputs = yaml.getConfigurationSection(base + "output-storage");
                if (outputs != null) {
                    for (String k : outputs.getKeys(false)) block.getOutputStorage().put(k, outputs.getInt(k));
                }

                factories.put(id, block);
                if (id >= nextId) nextId = id + 1;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load factory: " + key, e);
            }
        }
        plugin.getLogger().info("Loaded " + factories.size() + " factories.");
    }

    public void saveFactories() {
        if (factoriesFile == null) {
            factoriesFile = new File(plugin.getDataFolder(), "data/factories.yml");
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (FactoryBlock f : factories.values()) {
            String base = "factories." + f.getId() + ".";
            yaml.set(base + "world", f.getLocation().getWorld().getName());
            yaml.set(base + "x", f.getLocation().getX());
            yaml.set(base + "y", f.getLocation().getY());
            yaml.set(base + "z", f.getLocation().getZ());
            yaml.set(base + "owner", f.getOwnerUUID().toString());
            yaml.set(base + "company-id", f.getCompanyId());
            yaml.set(base + "recipe-id", f.getRecipeId());
            yaml.set(base + "health", f.getHealth());
            yaml.set(base + "fuel-buffer", f.getFuelBuffer());
            yaml.set(base + "status", f.getStatus().name());
            yaml.set(base + "production-start", f.getProductionStartMillis());
            f.getInputStorage().forEach((k, v) -> yaml.set(base + "input-storage." + k, v));
            f.getOutputStorage().forEach((k, v) -> yaml.set(base + "output-storage." + k, v));
        }
        try { yaml.save(factoriesFile); }
        catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Failed to save factories.yml", e); }
    }

    // ── Factory lifecycle ─────────────────────────────────────────────────────

    public FactoryBlock placeFactory(org.bukkit.entity.Player player, Location location,
                                     String recipeId, String companyId) {
        if (!recipes.containsKey(recipeId)) return null;
        long id = nextId++;
        FactoryBlock factory = new FactoryBlock(id, location.getBlock().getLocation(),
                player.getUniqueId(), companyId, recipeId);
        factories.put(id, factory);
        saveFactories();
        return factory;
    }

    public boolean removeFactory(long id) {
        if (factories.remove(id) == null) return false;
        saveFactories();
        return true;
    }

    // ── Production ────────────────────────────────────────────────────────────

    /**
     * Attempts to start production. Returns an error message, or null on success.
     */
    public String startProduction(FactoryBlock factory) {
        if (factory.getStatus() == FactoryBlock.Status.RUNNING) return "Factory is already running.";
        if (factory.getStatus() == FactoryBlock.Status.DAMAGED) return "Factory is too damaged to run (health < 20%).";

        FactoryRecipe recipe = recipes.get(factory.getRecipeId());
        if (recipe == null) return "Unknown recipe.";

        // Check fuel
        if (factory.getFuelBuffer() < recipe.getFuelAmount()) {
            return "Not enough fuel. Need " + recipe.getFuelAmount() + "x "
                    + formatName(recipe.getFuelMaterial().name()) + ".";
        }

        // Check inputs
        for (FactoryRecipe.RecipeStack input : recipe.getInputs()) {
            int stored = factory.getInputStorage().getOrDefault(input.material().name(), 0);
            if (stored < input.amount()) {
                return "Missing input: " + input.amount() + "x " + formatName(input.material().name())
                        + " (have " + stored + ").";
            }
        }

        // Consume fuel + inputs
        factory.setFuelBuffer(factory.getFuelBuffer() - recipe.getFuelAmount());
        for (FactoryRecipe.RecipeStack input : recipe.getInputs()) {
            int current = factory.getInputStorage().getOrDefault(input.material().name(), 0);
            int remaining = current - input.amount();
            if (remaining <= 0) factory.getInputStorage().remove(input.material().name());
            else factory.getInputStorage().put(input.material().name(), remaining);
        }

        // Adjust duration by throughput multiplier
        long durationMs = (long) (recipe.getDurationSeconds() * 1000L / factory.getThroughputMultiplier());
        factory.setStatus(FactoryBlock.Status.RUNNING);
        factory.setProductionStartMillis(System.currentTimeMillis());

        // Schedule completion
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (factory.getStatus() == FactoryBlock.Status.RUNNING) {
                completeProduction(factory, recipe);
            }
        }, durationMs / 50); // ms → ticks

        saveFactories();
        return null;
    }

    private void completeProduction(FactoryBlock factory, FactoryRecipe recipe) {
        factory.setStatus(FactoryBlock.Status.IDLE);
        factory.setProductionStartMillis(0);

        // Add outputs to storage
        for (FactoryRecipe.RecipeStack output : recipe.getOutputs()) {
            factory.getOutputStorage().merge(output.material().name(), output.amount(), Integer::sum);
        }

        saveFactories();

        // Notify owner if online
        org.bukkit.entity.Player owner = plugin.getServer().getPlayer(factory.getOwnerUUID());
        if (owner != null) {
            owner.sendMessage(color("&8[&6angelCreate&8] &aFactory &e" + recipe.getDisplayName()
                    + " &ahas finished production! Use &e/factory collect &ato pick up outputs."));
        }

        // Fire event and update market
        EventBus.publish(new ItemProducedEvent(
                factory.getCompanyId(),
                recipe.getId(),
                recipe.getOutputs().stream().mapToInt(FactoryRecipe.RecipeStack::amount).sum(),
                factory.getId()
        ));

        MarketService market = ServiceRegistry.getMarketService();
        if (market != null) {
            for (FactoryRecipe.RecipeStack output : recipe.getOutputs()) {
                market.recordTransaction(output.material().name(), output.amount(),
                        market.getPrice(output.material().name()));
            }
        }
    }

    // ── Damage / Repair ───────────────────────────────────────────────────────

    public void damageFactory(FactoryBlock factory, double amount) {
        double before = factory.getHealth();
        factory.setHealth(before - amount);

        if (factory.getHealth() < 20.0 && factory.getStatus() == FactoryBlock.Status.RUNNING) {
            factory.setStatus(FactoryBlock.Status.DAMAGED);
        }

        EventBus.publish(new FactoryDamagedEvent(
                factory.getId(),
                factory.getCompanyId(),
                (int) amount
        ));
        saveFactories();
    }

    public void repairFactory(FactoryBlock factory, double amount) {
        factory.setHealth(factory.getHealth() + amount);
        if (factory.getStatus() == FactoryBlock.Status.DAMAGED && factory.getHealth() >= 20.0) {
            factory.setStatus(FactoryBlock.Status.IDLE);
        }
        EventBus.publish(new FactoryRepairedEvent(
                factory.getId(),
                factory.getCompanyId(),
                (int) amount
        ));
        saveFactories();
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    public FactoryBlock getFactoryAt(Location location) {
        Location block = location.getBlock().getLocation();
        for (FactoryBlock f : factories.values()) {
            Location fl = f.getLocation().getBlock().getLocation();
            if (fl.getWorld().equals(block.getWorld())
                    && fl.getBlockX() == block.getBlockX()
                    && fl.getBlockY() == block.getBlockY()
                    && fl.getBlockZ() == block.getBlockZ()) {
                return f;
            }
        }
        return null;
    }

    public FactoryBlock getFactory(long id) { return factories.get(id); }
    public Collection<FactoryBlock> getAllFactories() { return factories.values(); }
    public List<FactoryBlock> getFactoriesForCompany(String companyId) {
        return factories.values().stream()
                .filter(f -> companyId.equals(f.getCompanyId()))
                .toList();
    }
    public Map<String, FactoryRecipe> getRecipes() { return recipes; }
    public FactoryRecipe getRecipe(String id) { return recipes.get(id); }

    // ── Tick (called from scheduler to poll running factories) ────────────────

    public void tickRunning() {
        long now = System.currentTimeMillis();
        for (FactoryBlock f : factories.values()) {
            if (f.getStatus() != FactoryBlock.Status.RUNNING) continue;
            FactoryRecipe recipe = recipes.get(f.getRecipeId());
            if (recipe == null) continue;
            long durationMs = (long) (recipe.getDurationSeconds() * 1000L / f.getThroughputMultiplier());
            if (now - f.getProductionStartMillis() >= durationMs) {
                completeProduction(f, recipe);
            }
        }
    }

    private String formatName(String key) {
        return Arrays.stream(key.toLowerCase().replace("_", " ").split(" "))
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .reduce("", (a, b) -> a + " " + b).trim();
    }

    private String color(String s) { return s.replace('&', '§'); }
}
