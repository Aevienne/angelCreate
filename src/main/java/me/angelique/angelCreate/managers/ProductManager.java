package me.angelique.angelCreate.managers;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.EffectModule;
import me.angelique.angelCreate.models.Ingredient;
import me.angelique.angelCreate.models.Product;
import me.angelique.angelCreate.models.enums.EffectType;
import me.angelique.angelCreate.models.enums.Trigger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ProductManager {

    private final AngelCreate plugin;
    private final Map<UUID, Product> products = new LinkedHashMap<>();
    private File dataFile;

    public ProductManager(AngelCreate plugin) { this.plugin = plugin; }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "data/products.yml");
        if (!dataFile.exists()) plugin.saveResource("data/products.yml", false);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        products.clear();

        if (!yaml.isConfigurationSection("products")) return;
        for (String key : yaml.getConfigurationSection("products").getKeys(false)) {
            try {
                String base = "products." + key + ".";
                UUID id = UUID.fromString(key);
                UUID companyId = UUID.fromString(yaml.getString(base + "company-id"));
                String displayName = yaml.getString(base + "display-name", "Unknown Product");
                Material mat = Material.matchMaterial(yaml.getString(base + "base-item", "PAPER"));
                if (mat == null) mat = Material.PAPER;

                Product p = new Product(id, companyId, displayName, mat);
                p.setCreatedAt(yaml.getLong(base + "created-at", 0));
                p.setLore(yaml.getStringList(base + "lore"));

                // ingredients
                if (yaml.isConfigurationSection(base + "ingredients")) {
                    for (String ikey : yaml.getConfigurationSection(base + "ingredients").getKeys(false)) {
                        String ibase = base + "ingredients." + ikey + ".";
                        Material imat = Material.matchMaterial(yaml.getString(ibase + "material", "AIR"));
                        int amount = yaml.getInt(ibase + "amount", 1);
                        if (imat != null) p.getIngredients().add(new Ingredient(imat, amount));
                    }
                }

                // effects
                if (yaml.isConfigurationSection(base + "effects")) {
                    for (String ekey : yaml.getConfigurationSection(base + "effects").getKeys(false)) {
                        EffectModule em = loadEffect(yaml, base + "effects." + ekey + ".");
                        if (em != null) p.getEffects().add(em);
                    }
                }

                products.put(id, p);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load product: " + key, e);
            }
        }
        plugin.getLogger().info("Loaded " + products.size() + " products.");
    }

    private EffectModule loadEffect(YamlConfiguration yaml, String base) {
        try {
            Trigger trigger = Trigger.valueOf(yaml.getString(base + "trigger"));
            EffectType type = EffectType.valueOf(yaml.getString(base + "type"));
            Map<String, Object> params = new HashMap<>();
            ConfigurationSection ps = yaml.getConfigurationSection(base + "parameters");
            if (ps != null) params.putAll(ps.getValues(true));
            // nested wrapped_effect for CHANCE_WRAPPER
            if (type == EffectType.CHANCE_WRAPPER && yaml.isConfigurationSection(base + "parameters.wrapped_effect")) {
                String we = base + "parameters.wrapped_effect.";
                EffectModule nested = loadEffect(yaml, we);
                if (nested != null) params.put("wrapped_effect", nested);
            }
            return new EffectModule(trigger, type, params);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load effect at " + base, e);
            return null;
        }
    }

    public void save() {
        if (dataFile == null) return;
        YamlConfiguration out = new YamlConfiguration();
        for (Product p : products.values()) {
            String base = "products." + p.getId() + ".";
            out.set(base + "company-id", p.getCompanyId().toString());
            out.set(base + "display-name", p.getDisplayName());
            out.set(base + "base-item", p.getBaseItem().name());
            out.set(base + "created-at", p.getCreatedAt());
            out.set(base + "lore", p.getLore());
            int ii = 0;
            for (Ingredient ing : p.getIngredients()) {
                out.set(base + "ingredients." + ii + ".material", ing.getMaterial().name());
                out.set(base + "ingredients." + ii + ".amount", ing.getAmount());
                ii++;
            }
            int ei = 0;
            for (EffectModule em : p.getEffects()) {
                saveEffect(out, base + "effects." + ei + ".", em);
                ei++;
            }
        }
        try { out.save(dataFile); }
        catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Failed to save products.yml", e); }
    }

    private void saveEffect(YamlConfiguration out, String base, EffectModule em) {
        out.set(base + "trigger", em.getTrigger().name());
        out.set(base + "type", em.getType().name());
        for (Map.Entry<String, Object> entry : em.getParameters().entrySet()) {
            if (entry.getValue() instanceof EffectModule nested) {
                saveEffect(out, base + "parameters." + entry.getKey() + ".", nested);
            } else {
                out.set(base + "parameters." + entry.getKey(), entry.getValue());
            }
        }
    }

    public Product createProduct(UUID companyId, String name, Material base) {
        UUID id = UUID.randomUUID();
        Product p = new Product(id, companyId, name, base);
        products.put(id, p);
        save();
        return p;
    }

    public void deleteProduct(UUID id) { products.remove(id); save(); }

    public Product getProduct(UUID id) { return products.get(id); }

    public List<Product> getProductsForCompany(UUID companyId) {
        List<Product> list = new ArrayList<>();
        for (Product p : products.values()) if (p.getCompanyId().equals(companyId)) list.add(p);
        return list;
    }

    public Collection<Product> getAllProducts() { return products.values(); }
}
