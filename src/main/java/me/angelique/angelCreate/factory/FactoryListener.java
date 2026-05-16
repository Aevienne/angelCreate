package me.angelique.angelCreate.factory;

import me.angelique.angelCreate.AngelCreate;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class FactoryListener implements Listener {

    private final AngelCreate plugin;

    public FactoryListener(AngelCreate plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        FactoryBlock factory = plugin.getFactoryManager().getFactoryAt(e.getClickedBlock().getLocation());
        if (factory == null) return;
        e.setCancelled(true);
        FactoryGUI.open(plugin, e.getPlayer(), factory);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        FactoryBlock factory = plugin.getFactoryManager().getFactoryAt(e.getBlock().getLocation());
        if (factory == null) return;
        if (!e.getPlayer().getUniqueId().equals(factory.getOwnerUUID())
                && !e.getPlayer().hasPermission("createecon.admin")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(color("&8[&6angelCreate&8] &cUse /factory remove to decommission your factory."));
            return;
        }
        e.setCancelled(true);
        e.getPlayer().sendMessage(color("&8[&6angelCreate&8] &eUse &6/factory remove &eto decommission this factory."));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().title().toString().contains("[Factory]")) return;
        e.setCancelled(true);
        FactoryGUI.handleClick(plugin, e);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        FactoryGUI.OPEN_GUIS.remove(e.getPlayer().getUniqueId());
    }

    private String color(String s) { return s.replace('&', '§'); }
}
