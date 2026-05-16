package me.angelique.angelCreate.factory;

import me.angelique.angelCreate.AngelCreate;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class FactoryGUI {

    private static final int SLOT_STATUS  = 4;
    private static final int SLOT_START   = 22;
    private static final int SLOT_COLLECT = 31;
    private static final int SLOT_FUEL    = 40;

    public static final Map<UUID, FactoryBlock> OPEN_GUIS = new HashMap<>();

    public static void open(AngelCreate plugin, Player player, FactoryBlock factory) {
        OPEN_GUIS.put(player.getUniqueId(), factory);
        player.openInventory(build(plugin, factory));
    }

    private static Inventory build(AngelCreate plugin, FactoryBlock factory) {
        FactoryRecipe recipe = plugin.getFactoryManager().getRecipe(factory.getRecipeId());
        String title = "§6[Factory] §f" + (recipe != null ? recipe.getDisplayName() : factory.getRecipeId());
        Inventory inv = plugin.getServer().createInventory(null, 54, Component.text(title));

        ItemStack filler = make(Material.GRAY_STAINED_GLASS_PANE, "§7");
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // Status panel
        String statusColor = switch (factory.getStatus()) {
            case RUNNING -> "§a";
            case DAMAGED -> "§c";
            default -> "§7";
        };
        inv.setItem(SLOT_STATUS, make(Material.COMPARATOR, statusColor + "Status: " + factory.getStatus().name(),
                "§7Health: §f" + String.format("%.0f", factory.getHealth()) + "%",
                "§7Fuel:   §f" + factory.getFuelBuffer() + " units"));

        if (recipe != null) {
            // Inputs row (slots 10-16)
            int slot = 10;
            for (FactoryRecipe.RecipeStack input : recipe.getInputs()) {
                int stored = factory.getInputStorage().getOrDefault(input.material().name(), 0);
                boolean ok = stored >= input.amount();
                inv.setItem(slot++, make(input.material(),
                        (ok ? "§a" : "§c") + formatName(input.material().name()),
                        "§7Need: §f" + input.amount(),
                        "§7Have: §f" + stored));
            }

            // Outputs row (slots 28-34)
            slot = 28;
            for (FactoryRecipe.RecipeStack output : recipe.getOutputs()) {
                int stored = factory.getOutputStorage().getOrDefault(output.material().name(), 0);
                inv.setItem(slot++, make(output.material(),
                        "§e" + formatName(output.material().name()),
                        "§7Produces: §f" + output.amount() + " per run",
                        "§7In storage: §f" + stored));
            }

            // Fuel slot
            inv.setItem(SLOT_FUEL, make(recipe.getFuelMaterial(),
                    "§6Fuel: " + formatName(recipe.getFuelMaterial().name()),
                    "§7Buffered: §f" + factory.getFuelBuffer() + " units",
                    "§7Needed:   §f" + recipe.getFuelAmount() + " per run",
                    "§eClick to add fuel from inventory"));
        }

        // Start button
        boolean canStart = factory.getStatus() == FactoryBlock.Status.IDLE;
        inv.setItem(SLOT_START, make(
                canStart ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA,
                canStart ? "§aSTART PRODUCTION" : "§cCannot Start",
                factory.getStatus() == FactoryBlock.Status.RUNNING ? "§7Already running." :
                factory.getStatus() == FactoryBlock.Status.DAMAGED ? "§cRepair the factory first." :
                "§7Click to begin a production run."));

        // Collect button
        boolean hasOutputs = factory.getOutputStorage().values().stream().anyMatch(v -> v > 0);
        inv.setItem(SLOT_COLLECT, make(
                hasOutputs ? Material.CHEST : Material.BARREL,
                hasOutputs ? "§aCOLLECT OUTPUTS" : "§7No outputs yet",
                hasOutputs ? "§7Click to collect all outputs." : "§7Run a production cycle first."));

        return inv;
    }

    public static void handleClick(AngelCreate plugin, InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        FactoryBlock factory = OPEN_GUIS.get(player.getUniqueId());
        if (factory == null) return;
        FactoryRecipe recipe = plugin.getFactoryManager().getRecipe(factory.getRecipeId());

        int slot = e.getSlot();
        if (slot == SLOT_START) {
            player.closeInventory();
            String err = plugin.getFactoryManager().startProduction(factory);
            if (err != null) {
                player.sendMessage(color("&8[&6angelCreate&8] &c" + err));
            } else {
                player.sendMessage(color("&8[&6angelCreate&8] &aProduction started!"));
            }
        } else if (slot == SLOT_COLLECT) {
            collectOutputs(plugin, player, factory);
            player.closeInventory();
        } else if (slot == SLOT_FUEL && recipe != null) {
            addFuel(plugin, player, factory, recipe);
            open(plugin, player, factory);
        } else if (slot >= 10 && slot <= 16 && recipe != null) {
            // Click on input slot — deposit that material from inventory
            int inputIndex = slot - 10;
            if (inputIndex < recipe.getInputs().size()) {
                FactoryRecipe.RecipeStack needed = recipe.getInputs().get(inputIndex);
                depositInput(plugin, player, factory, needed);
                open(plugin, player, factory);
            }
        }
    }

    private static void collectOutputs(AngelCreate plugin, Player player, FactoryBlock factory) {
        if (factory.getOutputStorage().isEmpty()) {
            player.sendMessage(color("&8[&6angelCreate&8] &7No outputs to collect."));
            return;
        }
        Map<String, Integer> outputs = new HashMap<>(factory.getOutputStorage());
        factory.getOutputStorage().clear();
        plugin.getFactoryManager().saveFactories();

        for (Map.Entry<String, Integer> entry : outputs.entrySet()) {
            Material mat = Material.matchMaterial(entry.getKey());
            if (mat == null) continue;
            int total = entry.getValue();
            while (total > 0) {
                int give = Math.min(total, mat.getMaxStackSize());
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(mat, give));
                leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                total -= give;
            }
        }
        player.sendMessage(color("&8[&6angelCreate&8] &aOutputs collected!"));
    }

    private static void addFuel(AngelCreate plugin, Player player, FactoryBlock factory, FactoryRecipe recipe) {
        Material fuelMat = recipe.getFuelMaterial();
        int count = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != fuelMat) continue;
            count += item.getAmount();
            player.getInventory().setItem(i, null);
        }
        if (count == 0) {
            player.sendMessage(color("&8[&6angelCreate&8] &cNo " + formatName(fuelMat.name()) + " in inventory."));
            return;
        }
        factory.setFuelBuffer(factory.getFuelBuffer() + count);
        plugin.getFactoryManager().saveFactories();
        player.sendMessage(color("&8[&6angelCreate&8] &aAdded §e" + count + " &afuel units."));
    }

    private static void depositInput(AngelCreate plugin, Player player, FactoryBlock factory,
                                     FactoryRecipe.RecipeStack needed) {
        Material mat = needed.material();
        int count = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != mat) continue;
            count += item.getAmount();
            player.getInventory().setItem(i, null);
        }
        if (count == 0) {
            player.sendMessage(color("&8[&6angelCreate&8] &cNo " + formatName(mat.name()) + " in inventory."));
            return;
        }
        factory.getInputStorage().merge(mat.name(), count, Integer::sum);
        plugin.getFactoryManager().saveFactories();
        player.sendMessage(color("&8[&6angelCreate&8] &aDeposited §e" + count + "x "
                + formatName(mat.name()) + "&a into factory."));
    }

    private static ItemStack make(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static String formatName(String key) {
        return Arrays.stream(key.toLowerCase().replace("_", " ").split(" "))
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .reduce("", (a, b) -> a + " " + b).trim();
    }

    private static String color(String s) { return s.replace('&', '§'); }
}
