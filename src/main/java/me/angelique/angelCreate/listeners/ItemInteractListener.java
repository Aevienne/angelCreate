package me.angelique.angelCreate.listeners;

import me.angelique.angelCreate.AngelCreate;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class ItemInteractListener implements Listener {

    private final AngelCreate plugin;

    public ItemInteractListener(AngelCreate plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        String title = e.getView().title().toString();
        // Lock all angelCreate GUIs — WorkbenchGUI and ProductSelectGUI handle their
        // own cancellation, this is a safety net for any missed slots.
        if (title.startsWith("\u00A76[angelCreate]")) {
            e.setCancelled(true);
        }
    }
}
