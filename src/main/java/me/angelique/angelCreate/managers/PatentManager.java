package me.angelique.angelCreate.managers;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.Patent;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class PatentManager {

    private final AngelCreate plugin;
    private final Map<UUID, Patent> patents = new LinkedHashMap<>();
    private File dataFile;

    public PatentManager(AngelCreate plugin) { this.plugin = plugin; }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "data/patents.yml");
        if (!dataFile.exists()) plugin.saveResource("data/patents.yml", false);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        patents.clear();

        if (!yaml.isConfigurationSection("patents")) return;
        for (String key : yaml.getConfigurationSection("patents").getKeys(false)) {
            try {
                String base = "patents." + key + ".";
                UUID id = UUID.fromString(key);
                UUID productId = UUID.fromString(yaml.getString(base + "product-id"));
                UUID companyId = UUID.fromString(yaml.getString(base + "company-id"));
                long issuedAt = yaml.getLong(base + "issued-at");
                long expiresAt = yaml.getLong(base + "expires-at");
                Patent p = new Patent(id, productId, companyId, issuedAt, expiresAt);
                if (!p.isExpired()) patents.put(id, p);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load patent: " + key, e);
            }
        }
        plugin.getLogger().info("Loaded " + patents.size() + " active patents.");
    }

    public void save() {
        if (dataFile == null) return;
        purgeExpired();
        YamlConfiguration out = new YamlConfiguration();
        for (Patent p : patents.values()) {
            String base = "patents." + p.getId() + ".";
            out.set(base + "product-id", p.getProductId().toString());
            out.set(base + "company-id", p.getCompanyId().toString());
            out.set(base + "issued-at", p.getIssuedAt());
            out.set(base + "expires-at", p.getExpiresAt());
        }
        try { out.save(dataFile); }
        catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Failed to save patents.yml", e); }
    }

    public void purgeExpired() {
        patents.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    public Patent issuePatent(UUID productId, UUID companyId) {
        // Remove any existing patent for this product first
        patents.entrySet().removeIf(e -> e.getValue().getProductId().equals(productId));
        UUID id = UUID.randomUUID();
        long now = System.currentTimeMillis();
        long durationDays = plugin.getConfig().getLong("economy.patent-duration-days", 7);
        long expiresAt = now + durationDays * 86400000L;
        Patent p = new Patent(id, productId, companyId, now, expiresAt);
        patents.put(id, p);
        save();
        return p;
    }

    public void forceExpire(UUID productId) {
        patents.entrySet().removeIf(e -> e.getValue().getProductId().equals(productId));
        save();
    }

    public Patent getPatentForProduct(UUID productId) {
        return patents.values().stream()
            .filter(p -> p.getProductId().equals(productId) && !p.isExpired())
            .findFirst().orElse(null);
    }

    public List<Patent> getPatentsForCompany(UUID companyId) {
        List<Patent> list = new ArrayList<>();
        for (Patent p : patents.values()) if (p.getCompanyId().equals(companyId) && !p.isExpired()) list.add(p);
        return list;
    }

    public Collection<Patent> getAllPatents() { return patents.values(); }
}
