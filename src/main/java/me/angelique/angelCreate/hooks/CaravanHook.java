package me.angelique.angelCreate.hooks;

import me.angelique.angelCreate.AngelCreate;
import org.bukkit.Bukkit;

public class CaravanHook {

    private final AngelCreate plugin;
    private boolean enabled = false;

    public CaravanHook(AngelCreate plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("angelTrade") != null) {
            this.enabled = true;
            plugin.getLogger().info("angelTrade detected — CaravanHook enabled (stub).");
            // TODO: register listeners or API hooks here when angelTrade exposes its API
        }
    }

    public boolean isEnabled() { return enabled; }
}
