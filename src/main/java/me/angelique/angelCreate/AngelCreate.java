// test build
package me.angelique.angelCreate;

import me.angelique.angelCreate.commands.AdminCommand;
import me.angelique.angelCreate.commands.CompanyCommand;
import me.angelique.angelCreate.commands.FactoryCommand;
import me.angelique.angelCreate.commands.PatentCommand;
import me.angelique.angelCreate.commands.ProductCommand;
import me.angelique.angelCreate.factory.FactoryListener;
import me.angelique.angelCreate.factory.FactoryManager;
import me.angelique.angelCreate.gui.CompanyDashboardGui;
import me.angelique.angelCreate.gui.CompanyDashboardListener;
import me.angelique.angelCreate.gui.ProductSelectGUI;
import me.angelique.angelCreate.gui.WorkbenchGUI;
import me.angelique.angelCreate.hooks.CaravanHook;
import me.angelique.angelCreate.listeners.EffectTriggerListener;
import me.angelique.angelCreate.listeners.ItemInteractListener;
import me.angelique.angelCreate.listeners.WorkbenchListener;
import me.angelique.angelCreate.listeners.SeasonProductionListener;
import me.angelique.angelCreate.managers.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.logging.Level;

public class AngelCreate extends JavaPlugin implements Listener {

    private static AngelCreate instance;

    private Economy economy;
    private CompanyManager companyManager;
    private ProductManager productManager;
    private PatentManager patentManager;
    private EffectManager effectManager;
    private CaravanHook caravanHook;
    private FactoryManager factoryManager;
    private YamlConfiguration effectsConfig;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadEffectsConfig();
        ensureDataFolder();

        if (!setupEconomy()) {
            getLogger().warning("Vault not found — monetary features will be disabled.");
        }

        companyManager = new CompanyManager(this);
        productManager = new ProductManager(this);
        patentManager  = new PatentManager(this);
        effectManager  = new EffectManager(this);
        caravanHook    = new CaravanHook(this);

        factoryManager = new FactoryManager(this);
        factoryManager.loadRecipes();
        factoryManager.loadFactories();

        companyManager.load();
        productManager.load();
        patentManager.load();

        // Purge expired patents on startup
        patentManager.purgeExpired();

        // Register events
        getServer().getPluginManager().registerEvents(new WorkbenchListener(this), this);
        getServer().getPluginManager().registerEvents(new EffectTriggerListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new FactoryListener(this), this);
        getServer().getPluginManager().registerEvents(new CompanyDashboardListener(this), this);
        getServer().getPluginManager().registerEvents(caravanHook, this);
        if (getServer().getPluginManager().getPlugin("AngelNCore") != null) {
            getServer().getPluginManager().registerEvents(new SeasonProductionListener(), this);
        }
        getServer().getPluginManager().registerEvents(this, this);

        // Register commands
        getCommand("company").setExecutor(new CompanyCommand(this));
        getCommand("product").setExecutor(new ProductCommand(this));
        getCommand("patent").setExecutor(new PatentCommand(this));
        getCommand("ceadmin").setExecutor(new AdminCommand(this));
        FactoryCommand factoryCmd = new FactoryCommand(this);
        getCommand("factory").setExecutor(factoryCmd);
        getCommand("factory").setTabCompleter(factoryCmd);

        // Patent expiry checker — every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    patentManager.purgeExpired();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Patent purge error", e);
                }
            }
        }.runTaskTimerAsynchronously(this, 6000L, 6000L);

        getLogger().info("angelCreate enabled.");
    }

    @Override
    public void onDisable() {
        // Final save on shutdown
        if (companyManager != null) companyManager.save();
        if (productManager != null)  productManager.save();
        if (patentManager != null)   patentManager.save();
        if (factoryManager != null)  factoryManager.saveFactories();
        getLogger().info("angelCreate disabled.");
    }

    // ── GUI click routing ─────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = e.getView().title().toString();
        if (title.startsWith("\u00A76[angelCreate]")) {
            e.setCancelled(true);
            if (title.contains("Workbench")) {
                WorkbenchGUI.handleClick(this, e);
            } else if (title.contains("Select Product")) {
                ProductSelectGUI.handleClick(this, e);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        WorkbenchGUI.OPEN_GUIS.remove(e.getPlayer().getUniqueId());
        ProductSelectGUI.OPEN_GUIS.remove(e.getPlayer().getUniqueId());
    }

    // ── Setup helpers ─────────────────────────────────────────────────────────

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    private void loadEffectsConfig() {
        File file = new File(getDataFolder(), "effects.yml");
        if (!file.exists()) saveResource("effects.yml", false);
        effectsConfig = YamlConfiguration.loadConfiguration(file);
    }

    private void ensureDataFolder() {
        new File(getDataFolder(), "data").mkdirs();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static AngelCreate getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public CompanyManager getCompanyManager() { return companyManager; }
    public ProductManager getProductManager() { return productManager; }
    public PatentManager getPatentManager() { return patentManager; }
    public EffectManager getEffectManager() { return effectManager; }
    public CaravanHook getCaravanHook() { return caravanHook; }
    public FactoryManager getFactoryManager() { return factoryManager; }
    public YamlConfiguration getEffectsConfig() { return effectsConfig; }
}
