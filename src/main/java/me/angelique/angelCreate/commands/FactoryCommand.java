package me.angelique.angelCreate.commands;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.factory.FactoryBlock;
import me.angelique.angelCreate.factory.FactoryGUI;
import me.angelique.angelCreate.factory.FactoryManager;
import me.angelique.angelCreate.factory.FactoryRecipe;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class FactoryCommand implements CommandExecutor, TabCompleter {

    private final AngelCreate plugin;

    public FactoryCommand(AngelCreate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        FactoryManager fm = plugin.getFactoryManager();

        switch (args[0].toLowerCase()) {
            case "place" -> {
                if (args.length < 2) { player.sendMessage(color("&cUsage: /factory place <recipe>")); return true; }
                String recipeId = args[1].toLowerCase();
                FactoryRecipe recipe = fm.getRecipe(recipeId);
                if (recipe == null) {
                    player.sendMessage(color("&cUnknown recipe. Available: &e"
                            + String.join(", ", fm.getRecipes().keySet())));
                    return true;
                }
                Block target = player.getTargetBlockExact(5);
                if (target == null || target.getType().isAir()) {
                    player.sendMessage(color("&cLook at a block to place the factory on."));
                    return true;
                }
                if (fm.getFactoryAt(target.getLocation()) != null) {
                    player.sendMessage(color("&cThere is already a factory at that block."));
                    return true;
                }
                // Determine company ID
                String companyId = "";
                var company = plugin.getCompanyManager().getCompanyOf(player.getUniqueId());
                if (company != null) companyId = company.getId().toString();

                FactoryBlock created = fm.placeFactory(player, target.getLocation(), recipeId, companyId);
                player.sendMessage(color("&aFactory &e" + recipe.getDisplayName()
                        + " &aplaced! (ID: " + created.getId() + ")"));
            }
            case "info" -> {
                Block target = player.getTargetBlockExact(5);
                FactoryBlock factory = target != null ? fm.getFactoryAt(target.getLocation()) : null;
                if (factory == null) { player.sendMessage(color("&cNo factory found.")); return true; }
                FactoryRecipe r = fm.getRecipe(factory.getRecipeId());
                player.sendMessage(color("&6Factory #" + factory.getId()
                        + " &7— &e" + (r != null ? r.getDisplayName() : factory.getRecipeId())));
                player.sendMessage(color("  &7Status: &f" + factory.getStatus()
                        + " &7Health: &f" + String.format("%.0f", factory.getHealth()) + "%"
                        + " &7Fuel: &f" + factory.getFuelBuffer()));
            }
            case "start" -> {
                Block target = player.getTargetBlockExact(5);
                FactoryBlock factory = target != null ? fm.getFactoryAt(target.getLocation()) : null;
                if (factory == null) { player.sendMessage(color("&cNo factory found.")); return true; }
                String err = fm.startProduction(factory);
                if (err != null) player.sendMessage(color("&c" + err));
                else player.sendMessage(color("&aProduction started!"));
            }
            case "collect" -> {
                Block target = player.getTargetBlockExact(5);
                FactoryBlock factory = target != null ? fm.getFactoryAt(target.getLocation()) : null;
                if (factory == null) { player.sendMessage(color("&cNo factory found.")); return true; }
                FactoryGUI.open(plugin, player, factory);
            }
            case "remove" -> {
                Block target = player.getTargetBlockExact(5);
                FactoryBlock factory = target != null ? fm.getFactoryAt(target.getLocation()) : null;
                if (factory == null) { player.sendMessage(color("&cNo factory found.")); return true; }
                if (!factory.getOwnerUUID().equals(player.getUniqueId())
                        && !player.hasPermission("createecon.admin")) {
                    player.sendMessage(color("&cYou don't own this factory."));
                    return true;
                }
                fm.removeFactory(factory.getId());
                player.sendMessage(color("&aFactory decommissioned."));
            }
            case "list" -> {
                String companyId = "";
                var company = plugin.getCompanyManager().getCompanyOf(player.getUniqueId());
                if (company != null) companyId = company.getId().toString();
                List<FactoryBlock> list = fm.getFactoriesForCompany(companyId);
                if (list.isEmpty()) { player.sendMessage(color("&7Your company has no factories.")); return true; }
                player.sendMessage(color("&6Your factories (" + list.size() + "):"));
                for (FactoryBlock f : list) {
                    FactoryRecipe r = fm.getRecipe(f.getRecipeId());
                    player.sendMessage(color("  &7#" + f.getId() + " &e"
                            + (r != null ? r.getDisplayName() : f.getRecipeId())
                            + " &7— " + f.getStatus().name()
                            + " [" + f.getLocation().getBlockX() + ","
                            + f.getLocation().getBlockY() + ","
                            + f.getLocation().getBlockZ() + "]"));
                }
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(color("&6/factory &7commands:"));
        player.sendMessage(color("  &e/factory place <recipe> &7— register a factory at the block you're looking at"));
        player.sendMessage(color("  &e/factory info           &7— show factory status"));
        player.sendMessage(color("  &e/factory start          &7— begin a production run"));
        player.sendMessage(color("  &e/factory collect        &7— open GUI to collect outputs"));
        player.sendMessage(color("  &e/factory remove         &7— decommission factory"));
        player.sendMessage(color("  &e/factory list           &7— list your company's factories"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return List.of("place", "info", "start", "collect", "remove", "list");
        if (args.length == 2 && args[0].equalsIgnoreCase("place"))
            return plugin.getFactoryManager().getRecipes().keySet().stream().toList();
        return List.of();
    }

    private String color(String s) { return s.replace('&', '§'); }
}
