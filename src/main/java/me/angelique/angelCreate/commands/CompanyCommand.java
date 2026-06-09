package me.angelique.angelCreate.commands;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.Company;
import me.angelique.angelCreate.models.enums.Role;
import me.angelique.angelNCore.events.CompanyIPOEvent;
import me.angelique.angelNCore.events.EventBus;
import me.angelique.angelNCore.services.ServiceRegistry;
import me.angelique.angelNCore.services.StockExchangeService;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class CompanyCommand implements CommandExecutor {

    private final AngelCreate plugin;

    public CompanyCommand(AngelCreate plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {

            case "create" -> {
                if (!player.hasPermission("createecon.company.create")) { noPerms(player); return true; }
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /company create <name>"); return true; }
                if (plugin.getCompanyManager().getCompanyOf(player.getUniqueId()) != null) {
                    player.sendMessage(p() + "&cYou are already in a company."); return true;
                }
                String name = args[1];
                if (plugin.getCompanyManager().getCompanyByName(name) != null) {
                    player.sendMessage(p() + "&cThat company name is already taken."); return true;
                }
                double cost = plugin.getConfig().getDouble("economy.company-create-cost", 1000.0);
                if (!vaultCheck(player)) return true;
                if (!plugin.getEconomy().has(player, cost)) {
                    player.sendMessage(p() + plugin.getConfig().getString("messages.insufficient-funds").replace('&','\u00A7'));
                    return true;
                }
                plugin.getEconomy().withdrawPlayer(player, cost);
                Company c = plugin.getCompanyManager().createCompany(name, player.getUniqueId());
                player.sendMessage(p() + plugin.getConfig().getString("messages.company-create-success",
                    "&aCompany &6{name} &aregistered!")
                    .replace("{name}", name).replace("{cost}", String.format("%.2f", cost)).replace('&','\u00A7'));
                player.sendMessage(p() + "&7Next: &e/company deposit <amount> &7to fund treasury, &e/company invite <player> &7to add workers.");
            }

            case "ipo" -> {
                Company c = requireCompany(player); if (c == null) return true;
                if (!c.getOwner().equals(player.getUniqueId())) { noPerms(player); return true; }
                if (c.isIpoListed()) { player.sendMessage(p() + "&eYour company is already IPO listed."); return true; }
                double cost = plugin.getConfig().getDouble("economy.ipo-cost", 5000.0);
                if (!vaultCheck(player)) return true;
                if (!plugin.getEconomy().has(player, cost)) {
                    player.sendMessage(p() + plugin.getConfig().getString("messages.insufficient-funds").replace('&','\u00A7'));
                    return true;
                }
                plugin.getEconomy().withdrawPlayer(player, cost);
                c.setIpoListed(true);
                plugin.getCompanyManager().save();
                player.sendMessage(p() + plugin.getConfig().getString("messages.ipo-success",
                    "&aCompany &6{name} &ais now publicly listed!")
                    .replace("{name}", c.getName()).replace('&','\u00A7'));
                player.sendMessage(p() + "&7View on the stock exchange: &bhttp://127.0.0.1:8080/app/");
                StockExchangeService sx = ServiceRegistry.getStockExchangeService();
                if (sx != null) {
                    int shares = 1000 * c.getLevel();
                    double price = Math.max(1.0, c.getTreasury() / Math.max(1, shares));
                    sx.listCompany(c.getId().toString(), c.getName(), shares, price);
                }
                EventBus.publish(new CompanyIPOEvent(c.getId().toString(), c.getName(), 1000 * c.getLevel(), Math.max(1.0, c.getTreasury() / Math.max(1, 1000 * c.getLevel()))));
            }

            case "unipo" -> {
                Company c = requireCompany(player); if (c == null) return true;
                if (!c.getOwner().equals(player.getUniqueId())) { noPerms(player); return true; }
                if (!c.isIpoListed()) { player.sendMessage(p() + "&eYour company is not IPO listed."); return true; }
                c.setIpoListed(false);
                plugin.getCompanyManager().save();
                player.sendMessage(p() + "&aCompany &6" + c.getName() + " &adelisted from the stock exchange.");
            }

            case "invite" -> {
                Company c = requireCompany(player); if (c == null) return true;
                if (!c.hasRole(player.getUniqueId(), Role.MANAGER)) { noPerms(player); return true; }
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /company invite <player>"); return true; }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) { player.sendMessage(p() + "&cPlayer not found."); return true; }
                if (plugin.getCompanyManager().getCompanyOf(target.getUniqueId()) != null) {
                    player.sendMessage(p() + "&c" + target.getName() + " is already in a company."); return true;
                }
                c.getMembers().put(target.getUniqueId(), Role.WORKER);
                plugin.getCompanyManager().save();
                player.sendMessage(p() + "&a" + target.getName() + " added as WORKER.");
                target.sendMessage(p() + "&aYou have been invited to &6" + c.getName() + "&a!");
            }

            case "kick" -> {
                Company c = requireCompany(player); if (c == null) return true;
                if (!c.hasRole(player.getUniqueId(), Role.MANAGER)) { noPerms(player); return true; }
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /company kick <player>"); return true; }
                OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
                if (!c.getMembers().containsKey(target.getUniqueId())) {
                    player.sendMessage(p() + "&cThat player is not in your company."); return true;
                }
                if (c.getOwner().equals(target.getUniqueId())) {
                    player.sendMessage(p() + "&cCannot kick the owner."); return true;
                }
                c.getMembers().remove(target.getUniqueId());
                plugin.getCompanyManager().save();
                player.sendMessage(p() + "&a" + target.getName() + " removed from company.");
            }

            case "promote" -> {
                Company c = requireCompany(player); if (c == null) return true;
                if (!c.getOwner().equals(player.getUniqueId())) { noPerms(player); return true; }
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /company promote <player>"); return true; }
                OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
                Role current = c.getMembers().get(target.getUniqueId());
                if (current == null) { player.sendMessage(p() + "&cNot a member."); return true; }
                if (current == Role.WORKER) {
                    c.getMembers().put(target.getUniqueId(), Role.MANAGER);
                    plugin.getCompanyManager().save();
                    player.sendMessage(p() + "&a" + target.getName() + " promoted to MANAGER.");
                } else { player.sendMessage(p() + "&eAlready MANAGER or higher."); }
            }

            case "demote" -> {
                Company c = requireCompany(player); if (c == null) return true;
                if (!c.getOwner().equals(player.getUniqueId())) { noPerms(player); return true; }
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /company demote <player>"); return true; }
                OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
                Role current = c.getMembers().get(target.getUniqueId());
                if (current == null) { player.sendMessage(p() + "&cNot a member."); return true; }
                if (current == Role.MANAGER) {
                    c.getMembers().put(target.getUniqueId(), Role.WORKER);
                    plugin.getCompanyManager().save();
                    player.sendMessage(p() + "&a" + target.getName() + " demoted to WORKER.");
                } else { player.sendMessage(p() + "&eAlready WORKER."); }
            }

            case "info" -> {
                Company c = args.length > 1
                    ? plugin.getCompanyManager().getCompanyByName(args[1])
                    : plugin.getCompanyManager().getCompanyOf(player.getUniqueId());
                if (c == null) { player.sendMessage(p() + "&cCompany not found."); return true; }
                player.sendMessage(p() + "&6--- " + c.getName() + " ---");
                player.sendMessage(p() + "&7Treasury: &e$" + String.format("%.2f", c.getTreasury()));
                player.sendMessage(p() + "&7Members: &e" + c.getMembers().size());
                player.sendMessage(p() + "&7Products: &e" + c.getProductIds().size());
                player.sendMessage(p() + "&7IPO Listed: &e" + c.isIpoListed());
                for (Map.Entry<UUID, Role> entry : c.getMembers().entrySet()) {
                    OfflinePlayer m = plugin.getServer().getOfflinePlayer(entry.getKey());
                    player.sendMessage(p() + "  &7" + (m.getName() != null ? m.getName() : entry.getKey()) + " &8— &e" + entry.getValue().name());
                }
            }

            case "setworkbench" -> {
                Company c = requireCompany(player); if (c == null) return true;
                if (!c.hasRole(player.getUniqueId(), Role.MANAGER)) { noPerms(player); return true; }
                Block target = player.getTargetBlockExact(5);
                Material wbType = Material.matchMaterial(
                    plugin.getConfig().getString("workbench.block-type", "CARTOGRAPHY_TABLE"));
                if (target == null || target.getType() != wbType) {
                    player.sendMessage(p() + "&cLook at a Cartography Table within 5 blocks."); return true;
                }
                c.setWorkbenchLocation(target.getLocation());
                plugin.getCompanyManager().save();
                player.sendMessage(p() + "&aWorkbench set at " + target.getX() + "," + target.getY() + "," + target.getZ());
            }

            case "deposit" -> {
                Company c = requireCompany(player); if (c == null) return true;
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /company deposit <amount>"); return true; }
                try {
                    double amount = Double.parseDouble(args[1]);
                    if (amount <= 0) throw new NumberFormatException();
                    if (!vaultCheck(player)) return true;
                    if (!plugin.getEconomy().has(player, amount)) {
                        player.sendMessage(p() + "&cInsufficient personal funds."); return true;
                    }
                    plugin.getEconomy().withdrawPlayer(player, amount);
                    c.setTreasury(c.getTreasury() + amount);
                    plugin.getCompanyManager().save();
                    player.sendMessage(p() + "&aDeposited &e$" + String.format("%.2f", amount) + " &ato treasury.");
                    player.sendMessage(p() + "&7When ready, use &e/company ipo &7to go public on the stock exchange.");
                } catch (NumberFormatException ex) { player.sendMessage(p() + "&cInvalid amount."); }
            }

            case "withdraw" -> {
                Company c = requireCompany(player); if (c == null) return true;
                if (!c.getOwner().equals(player.getUniqueId())) { noPerms(player); return true; }
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /company withdraw <amount>"); return true; }
                try {
                    double amount = Double.parseDouble(args[1]);
                    if (amount <= 0) throw new NumberFormatException();
                    if (c.getTreasury() < amount) {
                        player.sendMessage(p() + "&cTreasury has insufficient funds."); return true;
                    }
                    if (!vaultCheck(player)) return true;
                    c.setTreasury(c.getTreasury() - amount);
                    plugin.getEconomy().depositPlayer(player, amount);
                    plugin.getCompanyManager().save();
                    player.sendMessage(p() + "&aWithdrew &e$" + String.format("%.2f", amount) + " &afrom treasury.");
                } catch (NumberFormatException ex) { player.sendMessage(p() + "&cInvalid amount."); }
            }

            default -> sendHelp(player);
        }
        return true;
    }

    private Company requireCompany(Player p) {
        Company c = plugin.getCompanyManager().getCompanyOf(p.getUniqueId());
        if (c == null) p.sendMessage(p() + plugin.getConfig().getString("messages.no-company").replace('&','\u00A7'));
        return c;
    }

    private boolean vaultCheck(Player p) {
        if (plugin.getEconomy() == null) {
            p.sendMessage(p() + plugin.getConfig().getString("messages.no-vault").replace('&','\u00A7'));
            return false;
        }
        return true;
    }

    private void noPerms(Player p) { p.sendMessage(p() + "&cInsufficient role permissions."); }

    private void sendHelp(Player p) {
        p.sendMessage(p() + "&6--- Company Help ---");
        p.sendMessage(p() + "&e/company create <name> &7- Register a company");
        p.sendMessage(p() + "&e/company ipo &7- List company on stock exchange");
        p.sendMessage(p() + "&e/company unipo &7- Delist from stock exchange");
        p.sendMessage(p() + "&e/company invite <player> &7- Invite member");
        p.sendMessage(p() + "&e/company kick <player> &7- Remove member");
        p.sendMessage(p() + "&e/company promote/demote <player> &7- Change role");
        p.sendMessage(p() + "&e/company info [name] &7- View company info");
        p.sendMessage(p() + "&e/company setworkbench &7- Tag workbench block");
        p.sendMessage(p() + "&e/company deposit/withdraw <amount> &7- Treasury");
    }

    private String p() { return plugin.getConfig().getString("messages.prefix","&8[&6angelCreate&8] &r").replace('&','\u00A7'); }
}
