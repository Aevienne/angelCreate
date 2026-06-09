package me.angelique.angelCreate.commands;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.Company;
import me.angelique.angelCreate.models.Patent;
import me.angelique.angelCreate.models.Product;
import me.angelique.angelCreate.models.enums.Role;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PatentCommand implements CommandExecutor {

    private final AngelCreate plugin;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public PatentCommand(AngelCreate plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {

            case "buy" -> {
                Company c = requireOwner(player); if (c == null) return true;
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /patent buy <productId>"); return true; }
                Product prod = resolveProduct(player, c, args[1]); if (prod == null) return true;

                Patent existing = plugin.getPatentManager().getPatentForProduct(prod.getId());
                if (existing != null && existing.getCompanyId().equals(c.getId())) {
                    player.sendMessage(p() + "&eYou already hold a patent on this product."); return true;
                }
                if (existing != null) {
                    player.sendMessage(p() + "&cAnother company holds a patent on this product until " + sdf.format(new Date(existing.getExpiresAt())));
                    return true;
                }

                double cost = plugin.getConfig().getDouble("economy.patent-cost", 500.0);
                if (c.getTreasury() < cost) {
                    player.sendMessage(p() + plugin.getConfig().getString("messages.insufficient-funds").replace('&','\u00A7'));
                    return true;
                }
                c.setTreasury(c.getTreasury() - cost);
                plugin.getCompanyManager().save();
                Patent patent = plugin.getPatentManager().issuePatent(prod.getId(), c.getId());
                c.getPatentIds().add(patent.getId());
                plugin.getCompanyManager().save();
                player.sendMessage(p() + "&aPatent issued for &6" + prod.getDisplayName()
                    + " &auntil &e" + sdf.format(new Date(patent.getExpiresAt())));
            }

            case "info" -> {
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /patent info <productId>"); return true; }
                // Resolve product globally (any company)
                Product prod = resolveProductGlobal(args[1]);
                if (prod == null) { player.sendMessage(p() + "&cProduct not found."); return true; }
                Patent patent = plugin.getPatentManager().getPatentForProduct(prod.getId());
                if (patent == null) {
                    player.sendMessage(p() + "&7No active patent on &f" + prod.getDisplayName());
                } else {
                    Company holder = plugin.getCompanyManager().getCompany(patent.getCompanyId());
                    player.sendMessage(p() + "&6Patent on &f" + prod.getDisplayName());
                    player.sendMessage(p() + "  &7Holder: &e" + (holder != null ? holder.getName() : "Unknown"));
                    player.sendMessage(p() + "  &7Issued: &e" + sdf.format(new Date(patent.getIssuedAt())));
                    player.sendMessage(p() + "  &7Expires: &e" + sdf.format(new Date(patent.getExpiresAt())));
                }
            }

            case "list" -> {
                Company c = plugin.getCompanyManager().getCompanyOf(player.getUniqueId());
                if (c == null) { player.sendMessage(p() + plugin.getConfig().getString("messages.no-company").replace('&','\u00A7')); return true; }
                List<Patent> patents = plugin.getPatentManager().getPatentsForCompany(c.getId());
                if (patents.isEmpty()) { player.sendMessage(p() + "&7No active patents."); return true; }
                player.sendMessage(p() + "&6--- " + c.getName() + " Patents ---");
                for (Patent patent : patents) {
                    Product prod = plugin.getProductManager().getProduct(patent.getProductId());
                    String prodName = prod != null ? prod.getDisplayName() : patent.getProductId().toString().substring(0,8);
                    player.sendMessage(p() + "  &e" + prodName + " &7expires &f" + sdf.format(new Date(patent.getExpiresAt())));
                }
            }

            default -> sendHelp(player);
        }
        return true;
    }

    private Company requireOwner(Player p) {
        Company c = plugin.getCompanyManager().getCompanyOf(p.getUniqueId());
        if (c == null) { p.sendMessage(p() + plugin.getConfig().getString("messages.no-company").replace('&','\u00A7')); return null; }
        if (!c.getOwner().equals(p.getUniqueId())) { p.sendMessage(p() + "&cOnly the company owner can manage patents."); return null; }
        return c;
    }

    private Product resolveProduct(Player player, Company c, String partialId) {
        for (UUID pid : c.getProductIds()) {
            Product p = plugin.getProductManager().getProduct(pid);
            if (p != null && p.getId().toString().startsWith(partialId.toLowerCase())) return p;
        }
        player.sendMessage(p() + "&cProduct not found in your company.");
        return null;
    }

    private Product resolveProductGlobal(String partialId) {
        for (Product p : plugin.getProductManager().getAllProducts()) {
            if (p.getId().toString().startsWith(partialId.toLowerCase())) return p;
        }
        return null;
    }

    private void sendHelp(Player p) {
        p.sendMessage(p() + "&6--- Patent Help ---");
        p.sendMessage(p() + "&e/patent buy <productId> &7- Purchase patent from treasury");
        p.sendMessage(p() + "&e/patent info <productId> &7- Check patent status");
        p.sendMessage(p() + "&e/patent list &7- List your company's patents");
    }

    private String p() { return plugin.getConfig().getString("messages.prefix","&8[&6angelCreate&8] &r").replace('&','\u00A7'); }
}
