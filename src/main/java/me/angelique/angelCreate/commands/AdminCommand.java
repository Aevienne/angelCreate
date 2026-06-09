package me.angelique.angelCreate.commands;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.Company;
import me.angelique.angelCreate.models.Product;
import me.angelique.angelCreate.util.ItemBuilder;
import me.angelique.angelCreate.util.PDCUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class AdminCommand implements CommandExecutor {

    private final AngelCreate plugin;

    public AdminCommand(AngelCreate plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("createecon.admin")) { sender.sendMessage("No permission."); return true; }
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {

            case "reload" -> {
                plugin.reloadConfig();
                plugin.getCompanyManager().load();
                plugin.getProductManager().load();
                plugin.getPatentManager().load();
                sender.sendMessage(p("&aReloaded all data and config."));
            }

            case "give" -> {
                if (args.length < 3) { sender.sendMessage(p("&cUsage: /ceadmin give <player> <productId>")); return true; }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) { sender.sendMessage(p("&cPlayer not found.")); return true; }
                Product prod = null;
                for (Product p : plugin.getProductManager().getAllProducts()) {
                    if (p.getId().toString().startsWith(args[2].toLowerCase())) { prod = p; break; }
                }
                if (prod == null) { sender.sendMessage(p("&cProduct not found.")); return true; }
                var item = new ItemBuilder(prod.getBaseItem()).name(prod.getDisplayName()).lore(prod.getLore()).build();
                PDCUtil.setString(item, new NamespacedKey(plugin, "product_id"), prod.getId().toString());
                PDCUtil.setString(item, new NamespacedKey(plugin, "company_id"), prod.getCompanyId().toString());
                Map<Integer, ?> leftover = target.getInventory().addItem(item);
                if (!leftover.isEmpty()) target.getWorld().dropItemNaturally(target.getLocation(), item);
                sender.sendMessage(p("&aGave &6" + prod.getDisplayName() + " &ato &e" + target.getName()));
            }

            case "setbalance" -> {
                if (args.length < 3) { sender.sendMessage(p("&cUsage: /ceadmin setbalance <company> <amount>")); return true; }
                Company c = plugin.getCompanyManager().getCompanyByName(args[1]);
                if (c == null) { sender.sendMessage(p("&cCompany not found.")); return true; }
                try {
                    double amount = Double.parseDouble(args[2]);
                    c.setTreasury(amount);
                    plugin.getCompanyManager().save();
                    sender.sendMessage(p("&aTreasury of &6" + c.getName() + " &aset to &e$" + String.format("%.2f", amount)));
                } catch (NumberFormatException e) { sender.sendMessage(p("&cInvalid amount.")); }
            }

            case "deletecompany" -> {
                if (args.length < 2) { sender.sendMessage(p("&cUsage: /ceadmin deletecompany <name>")); return true; }
                Company c = plugin.getCompanyManager().getCompanyByName(args[1]);
                if (c == null) { sender.sendMessage(p("&cCompany not found.")); return true; }
                plugin.getCompanyManager().deleteCompany(c.getId());
                sender.sendMessage(p("&cCompany &6" + c.getName() + " &cdeleted."));
            }

            case "expirepatent" -> {
                if (args.length < 2) { sender.sendMessage(p("&cUsage: /ceadmin expirepatent <productId>")); return true; }
                Product prod = null;
                for (Product p : plugin.getProductManager().getAllProducts()) {
                    if (p.getId().toString().startsWith(args[1].toLowerCase())) { prod = p; break; }
                }
                if (prod == null) { sender.sendMessage(p("&cProduct not found.")); return true; }
                plugin.getPatentManager().forceExpire(prod.getId());
                sender.sendMessage(p("&aPatent for &6" + prod.getDisplayName() + " &aforce-expired."));
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(p("&6--- Admin Help ---"));
        s.sendMessage(p("&e/ceadmin reload"));
        s.sendMessage(p("&e/ceadmin give <player> <productId>"));
        s.sendMessage(p("&e/ceadmin setbalance <company> <amount>"));
        s.sendMessage(p("&e/ceadmin deletecompany <name>"));
        s.sendMessage(p("&e/ceadmin expirepatent <productId>"));
    }

    private String p(String message) { return (plugin.getConfig().getString("messages.prefix","&8[&6angelCreate&8] &r") + message).replace('&','\u00A7'); }
}
