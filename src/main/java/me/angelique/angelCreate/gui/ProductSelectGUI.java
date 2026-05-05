package me.angelique.angelCreate.gui;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.Company;
import me.angelique.angelCreate.models.Product;
import me.angelique.angelCreate.util.ItemBuilder;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.*;
import java.util.function.Consumer;

public class ProductSelectGUI {

    private final AngelCreate plugin;
    private final Company company;
    private final Player player;
    private final Consumer<Product> onSelect;
    private int page = 0;

    public static final Map<UUID, ProductSelectGUI> OPEN_GUIS = new HashMap<>();

    public ProductSelectGUI(AngelCreate plugin, Company company, Player player, Consumer<Product> onSelect) {
        this.plugin = plugin;
        this.company = company;
        this.player = player;
        this.onSelect = onSelect;
    }

    public void open() {
        OPEN_GUIS.put(player.getUniqueId(), this);
        player.openInventory(buildInventory());
    }

    private Inventory buildInventory() {
        Inventory inv = plugin.getServer().createInventory(null, 36,
            Component.text("\u00A76[angelCreate] \u00A7fSelect Product"));

        List<Product> products = plugin.getProductManager().getProductsForCompany(company.getId());
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&7").build();

        int perPage = 28;
        int start = page * perPage;
        int end = Math.min(start + perPage, products.size());

        for (int i = start; i < end; i++) {
            Product p = products.get(i);
            ItemStack item = new ItemBuilder(p.getBaseItem())
                .name(p.getDisplayName())
                .lore("&7Click to select", "&8ID: " + p.getId().toString().substring(0, 8))
                .build();
            inv.setItem(i - start, item);
        }

        int totalPages = (int) Math.ceil((double) products.size() / perPage);
        inv.setItem(27, page > 0
            ? new ItemBuilder(Material.ARROW).name("&ePrev Page").build() : filler);
        inv.setItem(35, page < totalPages - 1
            ? new ItemBuilder(Material.ARROW).name("&eNext Page").build() : filler);
        inv.setItem(31, new ItemBuilder(Material.BARRIER).name("&cClose").build());

        return inv;
    }

    public static void handleClick(AngelCreate plugin, InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        ProductSelectGUI gui = OPEN_GUIS.get(player.getUniqueId());
        if (gui == null) return;

        List<Product> products = plugin.getProductManager().getProductsForCompany(gui.company.getId());
        int perPage = 28;
        int start = gui.page * perPage;
        int totalPages = (int) Math.ceil((double) products.size() / perPage);
        int slot = e.getSlot();

        if (slot == 31) { player.closeInventory(); OPEN_GUIS.remove(player.getUniqueId()); return; }
        if (slot == 27 && gui.page > 0) { gui.page--; player.openInventory(gui.buildInventory()); return; }
        if (slot == 35 && gui.page < totalPages - 1) { gui.page++; player.openInventory(gui.buildInventory()); return; }

        int idx = start + slot;
        if (idx < products.size() && slot < 27) {
            Product selected = products.get(idx);
            OPEN_GUIS.remove(player.getUniqueId());
            player.closeInventory();
            gui.onSelect.accept(selected);
        }
    }
}
