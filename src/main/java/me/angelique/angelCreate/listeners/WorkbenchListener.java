package me.angelique.angelCreate.listeners;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.gui.WorkbenchGUI;
import me.angelique.angelCreate.models.Company;
import me.angelique.angelCreate.models.enums.Role;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class WorkbenchListener implements Listener {

    private final AngelCreate plugin;

    public WorkbenchListener(AngelCreate plugin) { this.plugin = plugin; }

    private String prefix() {
        return plugin.getConfig().getString("messages.prefix", "&8[&6angelCreate&8] &r").replace('&', '\u00A7');
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Material wbType = Material.matchMaterial(
            plugin.getConfig().getString("workbench.block-type", "CARTOGRAPHY_TABLE"));
        if (wbType == null || e.getClickedBlock().getType() != wbType) return;

        Player player = e.getPlayer();
        Company company = plugin.getCompanyManager().getCompanyAtWorkbench(e.getClickedBlock().getLocation());

        if (company == null) return; // unregistered cartography table, let vanilla handle

        e.setCancelled(true);

        if (!company.getMembers().containsKey(player.getUniqueId())) {
            player.sendMessage(prefix() + plugin.getConfig().getString("messages.not-member",
                "&cYou do not have access to this workbench.").replace('&', '\u00A7'));
            return;
        }

        new WorkbenchGUI(plugin, company, player).open();
    }
}
