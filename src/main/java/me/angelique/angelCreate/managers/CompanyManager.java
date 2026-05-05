package me.angelique.angelCreate.managers;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.Company;
import me.angelique.angelCreate.models.enums.Role;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class CompanyManager {

    private final AngelCreate plugin;
    private final Map<UUID, Company> companies = new LinkedHashMap<>();
    private File dataFile;
    private YamlConfiguration yaml;

    public CompanyManager(AngelCreate plugin) { this.plugin = plugin; }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "data/companies.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("data/companies.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(dataFile);
        companies.clear();

        if (!yaml.isConfigurationSection("companies")) return;
        for (String key : yaml.getConfigurationSection("companies").getKeys(false)) {
            try {
                String base = "companies." + key + ".";
                UUID id = UUID.fromString(key);
                String name = yaml.getString(base + "name");
                UUID owner = UUID.fromString(yaml.getString(base + "owner"));
                Company c = new Company(id, name, owner);
                c.setTreasury(yaml.getDouble(base + "treasury", 0));
                c.setLevel(yaml.getInt(base + "level", 1));
                c.setIpoListed(yaml.getBoolean(base + "ipo-listed", false));

                // members
                if (yaml.isConfigurationSection(base + "members")) {
                    for (String m : yaml.getConfigurationSection(base + "members").getKeys(false)) {
                        UUID muuid = UUID.fromString(m);
                        Role role = Role.valueOf(yaml.getString(base + "members." + m, "WORKER"));
                        c.getMembers().put(muuid, role);
                    }
                }

                // productIds
                List<String> pids = yaml.getStringList(base + "products");
                for (String pid : pids) c.getProductIds().add(UUID.fromString(pid));

                // patentIds
                List<String> patids = yaml.getStringList(base + "patents");
                for (String pid : patids) c.getPatentIds().add(UUID.fromString(pid));

                // workbench
                if (yaml.isConfigurationSection(base + "workbench")) {
                    String world = yaml.getString(base + "workbench.world");
                    double x = yaml.getDouble(base + "workbench.x");
                    double y = yaml.getDouble(base + "workbench.y");
                    double z = yaml.getDouble(base + "workbench.z");
                    org.bukkit.World w = plugin.getServer().getWorld(world);
                    if (w != null) c.setWorkbenchLocation(new Location(w, x, y, z));
                }

                companies.put(id, c);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load company: " + key, e);
            }
        }
        plugin.getLogger().info("Loaded " + companies.size() + " companies.");
    }

    public void save() {
        if (dataFile == null) return;
        YamlConfiguration out = new YamlConfiguration();
        for (Company c : companies.values()) {
            String base = "companies." + c.getId() + ".";
            out.set(base + "name", c.getName());
            out.set(base + "owner", c.getOwner().toString());
            out.set(base + "treasury", c.getTreasury());
            out.set(base + "level", c.getLevel());
            out.set(base + "ipo-listed", c.isIpoListed());
            for (Map.Entry<UUID, Role> e : c.getMembers().entrySet()) {
                out.set(base + "members." + e.getKey(), e.getValue().name());
            }
            List<String> pids = new ArrayList<>();
            for (UUID pid : c.getProductIds()) pids.add(pid.toString());
            out.set(base + "products", pids);
            List<String> patids = new ArrayList<>();
            for (UUID pid : c.getPatentIds()) patids.add(pid.toString());
            out.set(base + "patents", patids);
            if (c.getWorkbenchLocation() != null) {
                Location l = c.getWorkbenchLocation();
                out.set(base + "workbench.world", l.getWorld().getName());
                out.set(base + "workbench.x", l.getX());
                out.set(base + "workbench.y", l.getY());
                out.set(base + "workbench.z", l.getZ());
            }
        }
        try { out.save(dataFile); }
        catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Failed to save companies.yml", e); }
    }

    public Company createCompany(String name, UUID owner) {
        UUID id = UUID.randomUUID();
        Company c = new Company(id, name, owner);
        companies.put(id, c);
        save();
        return c;
    }

    public void deleteCompany(UUID id) {
        companies.remove(id);
        save();
    }

    public Company getCompany(UUID id) { return companies.get(id); }

    public Company getCompanyByName(String name) {
        return companies.values().stream()
            .filter(c -> c.getName().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }

    public Company getCompanyOf(UUID playerUUID) {
        return companies.values().stream()
            .filter(c -> c.getMembers().containsKey(playerUUID))
            .findFirst().orElse(null);
    }

    public Company getCompanyAtWorkbench(Location loc) {
        for (Company c : companies.values()) {
            Location wb = c.getWorkbenchLocation();
            if (wb == null) continue;
            if (wb.getWorld().equals(loc.getWorld())
                && wb.getBlockX() == loc.getBlockX()
                && wb.getBlockY() == loc.getBlockY()
                && wb.getBlockZ() == loc.getBlockZ()) return c;
        }
        return null;
    }

    public Collection<Company> getAllCompanies() { return companies.values(); }
}
