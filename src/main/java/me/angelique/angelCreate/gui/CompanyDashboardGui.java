package me.angelique.angelCreate.gui;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.Company;
import me.angelique.angelCreate.models.enums.Role;
import me.angelique.angelNCore.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class CompanyDashboardGui {

    public static final String TITLE_PREFIX = TextUtil.color("&6Company: ");
    static final int SIZE = 45;

    private CompanyDashboardGui() {}

    public static void open(Player player, AngelCreate plugin) {
        Company c = plugin.getCompanyManager().getCompanyOf(player.getUniqueId());
        if (c == null) {
            openNoCompany(player, plugin);
            return;
        }
        openDashboard(player, plugin, c);
    }

    private static void openNoCompany(Player player, AngelCreate plugin) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TextUtil.color("&cCompany: None"));
        fillBorder(inv);

        inv.setItem(13, item(Material.BARRIER, "&cYou are not in a company",
                "&7Use &e/company create <name> &7to start one",
                "&7Cost: $" + String.format("%.0f", plugin.getConfig().getDouble("economy.company-create-cost", 1000.0))));

        inv.setItem(22, item(Material.EMERALD, "&aCreate Company",
                "&7Register a new company",
                "&7Cost: $" + String.format("%.0f", plugin.getConfig().getDouble("economy.company-create-cost", 1000.0)),
                "",
                "&eClick for command help"));
        inv.setItem(31, item(Material.KNOWLEDGE_BOOK, "&eLearn More",
                "&7Companies can create products,",
                "&7own factories, hold patents,",
                "&7and go public via IPO"));
        inv.setItem(40, item(Material.BARRIER, "&cClose"));

        player.openInventory(inv);
    }

    private static void openDashboard(Player player, AngelCreate plugin, Company c) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TextUtil.color(TITLE_PREFIX + c.getName()));
        fillBorder(inv);

        boolean isOwner = c.getOwner().equals(player.getUniqueId());
        boolean isManager = isOwner || c.hasRole(player.getUniqueId(), Role.MANAGER);

        // Row 1: Company info
        inv.setItem(10, item(Material.OAK_SIGN, "&6" + c.getName(),
                "&7Treasury: &e$" + String.format("%.2f", c.getTreasury()),
                "&7Members: &e" + c.getMembers().size(),
                "&7Products: &e" + c.getProductIds().size(),
                "&7Patents: &e" + c.getPatentIds().size(),
                "&7IPO: " + (c.isIpoListed() ? "&aListed" : "&7Private"),
                "&7Level: &e" + c.getLevel()));

        // Products
        inv.setItem(12, item(Material.PAPER, "&eProducts &7(" + c.getProductIds().size() + ")",
                "&7View & manage product catalog",
                "&7Create custom items with effects",
                "",
                "&eClick to manage"));
        // Factories
        inv.setItem(13, item(Material.FURNACE, "&7Factories",
                "&7Manage your production facilities",
                "&7View output & fuel status",
                "",
                "&eClick to view"));
        // Patents
        inv.setItem(14, item(Material.ENCHANTED_BOOK, "&dPatents &7(" + c.getPatentIds().size() + ")",
                "&7View held patents & expiration",
                "&7Protect your product IP",
                "",
                "&eClick to view"));
        // Workbench
        inv.setItem(15, item(Material.CARTOGRAPHY_TABLE, "&bWorkbench",
                c.getWorkbenchLocation() != null
                    ? "&7Location: &f" + c.getWorkbenchLocation().getBlockX() + "," + c.getWorkbenchLocation().getBlockY() + "," + c.getWorkbenchLocation().getBlockZ()
                    : "&cNot set",
                "",
                "&eUsage: &f/company setworkbench"));

        // Row 2: Treasury
        inv.setItem(20, item(Material.GOLD_INGOT, "&eDeposit",
                "&7Add personal funds to treasury",
                "&7Usage: /company deposit <amount>",
                "",
                "&eClick for help"));
        inv.setItem(21, item(Material.GOLD_NUGGET, "&eWithdraw",
                isOwner ? "&7Take funds from treasury" : "&cOwner only",
                "&7Usage: /company withdraw <amount>",
                "",
                isOwner ? "&eClick for help" : "&cOwner only"));
        inv.setItem(22, item(c.isIpoListed() ? Material.GOLD_BLOCK : Material.IRON_INGOT,
                c.isIpoListed() ? "&aIPO Listed \u2713" : "&7Go Public (IPO)",
                c.isIpoListed() ? "&7View on stock exchange" : "&7List your company on the stock exchange",
                c.isIpoListed() ? "&7Use /company unipo to delist" : "&7Cost: $" + String.format("%.0f", plugin.getConfig().getDouble("economy.ipo-cost", 5000.0)),
                "",
                isOwner ? "&eClick for help" : "&cOwner only"));

        // Row 2 continued: Members
        inv.setItem(23, item(Material.PLAYER_HEAD, "&aInvite Member",
                isManager ? "&7Add a player as WORKER" : "&cManager+ only",
                "&7Usage: /company invite <player>",
                "",
                isManager ? "&eClick for help" : ""));
        inv.setItem(24, item(Material.ARROW, "&aPromote",
                isOwner ? "&7Promote WORKER to MANAGER" : "&cOwner only",
                "&7Usage: /company promote <player>",
                "",
                isOwner ? "&eClick for help" : ""));

        // Row 3: Members list
        int slot = 28;
        for (Map.Entry<UUID, Role> entry : c.getMembers().entrySet()) {
            if (slot > 34) break;
            OfflinePlayer m = Bukkit.getOfflinePlayer(entry.getKey());
            boolean isOwnerMember = entry.getKey().equals(c.getOwner());
            String rolePrefix = isOwnerMember ? "&6[OWNER] " : entry.getValue() == Role.MANAGER ? "&a[MGR] " : "&7[WORKER] ";
            inv.setItem(slot++, skull(entry.getKey(), rolePrefix + (m.getName() != null ? m.getName() : "Unknown")));
        }

        // Row 4: Navigation
        inv.setItem(36, item(Material.EMERALD, "&aServer Shop", "&7Open the shop", "", "&eClick to open"));
        inv.setItem(40, item(Material.OAK_DOOR, "&cBack to Hub", "&7Return to main menu"));
        inv.setItem(44, item(Material.BARRIER, "&cClose"));

        player.openInventory(inv);
    }

    static void fillBorder(Inventory inv) {
        ItemStack glass = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, glass);
    }

    static ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.color(name));
            meta.setLore(Arrays.stream(lore).map(TextUtil::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private static ItemStack skull(UUID uuid, String name) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.color(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
