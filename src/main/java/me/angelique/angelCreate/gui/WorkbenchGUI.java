package me.angelique.angelCreate.gui;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.Company;
import me.angelique.angelCreate.models.Ingredient;
import me.angelique.angelCreate.models.Patent;
import me.angelique.angelCreate.models.Product;
import me.angelique.angelCreate.util.ItemBuilder;
import me.angelique.angelCreate.util.PDCUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class WorkbenchGUI {

    private final AngelCreate plugin;
    private final Company company;
    private final Player player;
    private final NamespacedKey productIdKey;
    private final NamespacedKey companyIdKey;

    private int page = 0;
    private Product selectedProduct = null;

    // GUI layout constants
    private static final int ROW_PRODUCTS_START = 0;  // slots 0-4
    private static final int SLOT_PREV = 9;
    private static final int SLOT_NEXT = 17;
    private static final int ROW_DIVIDER = 18;        // slot 18 — divider bar
    private static final int ROW_INGREDIENTS_START = 27; // slots 27-34
    private static final int SLOT_CRAFT = 35;

    // Track open GUIs by player UUID
    public static final Map<UUID, WorkbenchGUI> OPEN_GUIS = new HashMap<>();

    public WorkbenchGUI(AngelCreate plugin, Company company, Player player) {
        this.plugin = plugin;
        this.company = company;
        this.player = player;
        this.productIdKey = new NamespacedKey(plugin, "product_id");
        this.companyIdKey = new NamespacedKey(plugin, "company_id");
    }

    public void open() {
        OPEN_GUIS.put(player.getUniqueId(), this);
        player.openInventory(buildInventory());
    }

    private Inventory buildInventory() {
        Inventory inv = plugin.getServer().createInventory(null, 36,
            Component.text("\u00A76[angelCreate] \u00A7f" + company.getName() + " Workbench"));

        List<Product> products = plugin.getProductManager().getProductsForCompany(company.getId());
        int perPage = 5;
        int start = page * perPage;
        int end = Math.min(start + perPage, products.size());

        // Row 0: Product slots 0-4
        for (int i = start; i < end; i++) {
            Product p = products.get(i);
            ItemStack item = new ItemBuilder(p.getBaseItem())
                .name(p.getDisplayName())
                .lore(buildProductLore(p))
                .build();
            PDCUtil.setString(item, productIdKey, p.getId().toString());
            inv.setItem(i - start, item);
        }
        // Filler for empty product slots
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&7").build();
        for (int i = (end - start); i < 5; i++) inv.setItem(i, filler);

        // Row 1: Navigation (slots 9-17)
        if (page > 0)
            inv.setItem(SLOT_PREV, new ItemBuilder(Material.ARROW).name("&ePrev Page").build());
        else
            inv.setItem(SLOT_PREV, filler);

        // Slots 10-16 filler
        for (int i = 10; i <= 16; i++) inv.setItem(i, filler);

        int totalPages = (int) Math.ceil((double) products.size() / perPage);
        if (page < totalPages - 1)
            inv.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW).name("&eNext Page").build());
        else
            inv.setItem(SLOT_NEXT, filler);

        // Row 2: Divider (slots 18-26)
        ItemStack divider = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("&8── Selected Product ──").build();
        for (int i = 18; i <= 26; i++) inv.setItem(i, divider);

        // Row 3+4: Ingredients + Craft button (slots 27-35)
        if (selectedProduct != null) {
            List<Ingredient> ings = selectedProduct.getIngredients();
            for (int i = 0; i < Math.min(ings.size(), 8); i++) {
                Ingredient ing = ings.get(i);
                boolean has = hasIngredient(ing);
                ItemStack ingItem = new ItemBuilder(ing.getMaterial())
                    .name((has ? "&a" : "&c") + formatName(ing.getMaterial().name()))
                    .lore("&7Amount needed: &f" + ing.getAmount(),
                          has ? "&aYou have enough" : "&cInsufficient")
                    .build();
                inv.setItem(27 + i, ingItem);
            }
            // CRAFT button slot 35
            boolean canCraft = canCraftProduct(selectedProduct);
            inv.setItem(SLOT_CRAFT, new ItemBuilder(canCraft ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA)
                .name(canCraft ? "&aCRAFT" : "&cCannot Craft")
                .lore("&7" + selectedProduct.getDisplayName())
                .build());
        } else {
            ItemStack hint = new ItemBuilder(Material.OAK_SIGN)
                .name("&eSelect a product above")
                .lore("&7Click a product in Row 1 to see ingredients.").build();
            inv.setItem(31, hint);
        }

        return inv;
    }

    public static void handleClick(AngelCreate plugin, InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        WorkbenchGUI gui = OPEN_GUIS.get(player.getUniqueId());
        if (gui == null) return;
        gui.handleSlotClick(e.getSlot());
    }

    private void handleSlotClick(int slot) {
        List<Product> products = plugin.getProductManager().getProductsForCompany(company.getId());
        int perPage = 5;
        int start = page * perPage;
        int totalPages = (int) Math.ceil((double) products.size() / perPage);

        if (slot >= 0 && slot < 5) {
            // Product selection
            int idx = start + slot;
            if (idx < products.size()) {
                selectedProduct = products.get(idx);
                player.openInventory(buildInventory());
            }
        } else if (slot == SLOT_PREV && page > 0) {
            page--;
            selectedProduct = null;
            player.openInventory(buildInventory());
        } else if (slot == SLOT_NEXT && page < totalPages - 1) {
            page++;
            selectedProduct = null;
            player.openInventory(buildInventory());
        } else if (slot == SLOT_CRAFT && selectedProduct != null) {
            tryCraft();
        }
    }

    private void tryCraft() {
        // Patent check
        Patent patent = plugin.getPatentManager().getPatentForProduct(selectedProduct.getId());
        if (patent != null && !patent.getCompanyId().equals(company.getId())) {
            Company patentHolder = plugin.getCompanyManager().getCompany(patent.getCompanyId());
            String holderName = patentHolder != null ? patentHolder.getName() : "Unknown";
            String expDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(patent.getExpiresAt()));
            player.sendMessage(prefix() + plugin.getConfig().getString("messages.patent-blocked",
                "&cPatented by {company}. Expires: {date}.")
                .replace("{company}", holderName).replace("{date}", expDate).replace('&', '\u00A7'));
            player.closeInventory();
            return;
        }

        // Ingredient check
        if (!canCraftProduct(selectedProduct)) {
            player.sendMessage(msg("&cYou are missing ingredients."));
            player.openInventory(buildInventory());
            return;
        }

        // Consume ingredients
        for (Ingredient ing : selectedProduct.getIngredients()) {
            removeFromInventory(player, ing.getMaterial(), ing.getAmount());
        }

        // Build item
        ItemStack result = new ItemBuilder(selectedProduct.getBaseItem())
            .name(selectedProduct.getDisplayName())
            .lore(selectedProduct.getLore())
            .build();
        PDCUtil.setString(result, new NamespacedKey(plugin, "product_id"), selectedProduct.getId().toString());
        PDCUtil.setString(result, new NamespacedKey(plugin, "company_id"), selectedProduct.getCompanyId().toString());

        // Give item
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(result);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage(msg("&eInventory full — item dropped at your feet."));
        } else {
            player.sendMessage(msg("&aCrafted &6" + selectedProduct.getDisplayName() + "&a!"));
        }

        player.closeInventory();
        OPEN_GUIS.remove(player.getUniqueId());
    }

    private boolean canCraftProduct(Product p) {
        for (Ingredient ing : p.getIngredients()) {
            if (!hasIngredient(ing)) return false;
        }
        return true;
    }

    private boolean hasIngredient(Ingredient ing) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == ing.getMaterial()) count += item.getAmount();
        }
        return count >= ing.getAmount();
    }

    private void removeFromInventory(Player player, Material mat, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != mat) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
        }
    }

    private List<String> buildProductLore(Product p) {
        List<String> lore = new ArrayList<>(p.getLore());
        lore.add("&8---");
        lore.add("&7Ingredients: &f" + p.getIngredients().size());
        lore.add("&7Effects: &f" + p.getEffects().size());
        lore.add("&eClick to select");
        return lore;
    }

    private String formatName(String key) {
        String[] words = key.toLowerCase().replace("_", " ").split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }

    private String prefix() {
        return plugin.getConfig().getString("messages.prefix", "&8[&6angelCreate&8] &r").replace('&', '\u00A7');
    }

    private String msg(String message) {
        return prefix() + message.replace('&', '\u00A7');
    }
}
