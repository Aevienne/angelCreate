package me.angelique.angelCreate.gui;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.Company;
import me.angelique.angelNCore.util.TextUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CompanyDashboardListener implements Listener {

    private final AngelCreate plugin;

    public CompanyDashboardListener(AngelCreate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith("§6Company:")) return;
        event.setCancelled(true);

        Company c = plugin.getCompanyManager().getCompanyOf(player.getUniqueId());
        int slot = event.getSlot();

        if (title.contains("None")) {
            if (slot == 22) { player.closeInventory(); player.chat("/company create"); }
            if (slot == 40) player.closeInventory();
            return;
        }

        if (c == null) return;

        switch (slot) {
            case 12 -> { player.closeInventory(); player.performCommand("product"); }
            case 13 -> { player.closeInventory(); player.performCommand("factory list"); }
            case 14 -> { player.closeInventory(); player.performCommand("patent list"); }
            case 15 -> { player.closeInventory(); player.chat("/company setworkbench"); }
            case 20 -> { player.closeInventory(); player.chat("/company deposit "); }
            case 21 -> { player.closeInventory(); player.chat("/company withdraw "); }
            case 22 -> {
                player.closeInventory();
                if (c.getOwner().equals(player.getUniqueId())) {
                    player.chat("/company ipo");
                } else {
                    player.sendMessage(TextUtil.color("&cOnly the owner can manage IPO."));
                }
            }
            case 23 -> { player.closeInventory(); player.chat("/company invite "); }
            case 24 -> { player.closeInventory(); player.chat("/company promote "); }
            case 36 -> player.performCommand("shop");
            case 40 -> player.performCommand("menu");
            case 44 -> player.closeInventory();
        }
    }
}
