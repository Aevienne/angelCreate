package me.angelique.angelCreate.commands;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.Company;
import me.angelique.angelCreate.models.EffectModule;
import me.angelique.angelCreate.models.Ingredient;
import me.angelique.angelCreate.models.Product;
import me.angelique.angelCreate.models.enums.EffectType;
import me.angelique.angelCreate.models.enums.Role;
import me.angelique.angelCreate.models.enums.Trigger;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class ProductCommand implements CommandExecutor {

    private final AngelCreate plugin;

    public ProductCommand(AngelCreate plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {

            case "create" -> {
                Company c = requireCompany(player, Role.MANAGER); if (c == null) return true;
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /product create <name>"); return true; }
                String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                int maxEffects = plugin.getConfig().getInt("product-limits.max-effects", 5);
                List<Product> existing = plugin.getProductManager().getProductsForCompany(c.getId());
                // No hard cap on products, just effects per product
                Product prod = plugin.getProductManager().createProduct(c.getId(), name, Material.PAPER);
                c.getProductIds().add(prod.getId());
                plugin.getCompanyManager().save();
                player.sendMessage(p() + "&aProduct created: &6" + name + " &7ID: &e" + prod.getId().toString().substring(0,8));
            }

            case "setbase" -> {
                Company c = requireCompany(player, Role.MANAGER); if (c == null) return true;
                if (args.length < 3) { player.sendMessage(p() + "&cUsage: /product setbase <id> <material>"); return true; }
                Product prod = resolveProduct(player, c, args[1]); if (prod == null) return true;
                Material mat = Material.matchMaterial(args[2].toUpperCase());
                if (mat == null) { player.sendMessage(p() + "&cUnknown material."); return true; }
                prod.setBaseItem(mat);
                plugin.getProductManager().save();
                player.sendMessage(p() + "&aBase item set to &e" + mat.name());
            }

            case "addingredient" -> {
                Company c = requireCompany(player, Role.MANAGER); if (c == null) return true;
                if (args.length < 4) { player.sendMessage(p() + "&cUsage: /product addingredient <id> <material> <amount>"); return true; }
                Product prod = resolveProduct(player, c, args[1]); if (prod == null) return true;
                int max = plugin.getConfig().getInt("product-limits.max-ingredients", 9);
                if (prod.getIngredients().size() >= max) {
                    player.sendMessage(p() + "&cMax ingredients reached (" + max + ")."); return true;
                }
                Material mat = Material.matchMaterial(args[2].toUpperCase());
                if (mat == null) { player.sendMessage(p() + "&cUnknown material."); return true; }
                int amount;
                try { amount = Integer.parseInt(args[3]); if (amount <= 0) throw new NumberFormatException(); }
                catch (NumberFormatException e) { player.sendMessage(p() + "&cInvalid amount."); return true; }
                prod.getIngredients().add(new Ingredient(mat, amount));
                plugin.getProductManager().save();
                player.sendMessage(p() + "&aAdded ingredient: &e" + amount + "x " + mat.name());
            }

            case "removeingredient" -> {
                Company c = requireCompany(player, Role.MANAGER); if (c == null) return true;
                if (args.length < 3) { player.sendMessage(p() + "&cUsage: /product removeingredient <id> <index>"); return true; }
                Product prod = resolveProduct(player, c, args[1]); if (prod == null) return true;
                try {
                    int idx = Integer.parseInt(args[2]);
                    if (idx < 0 || idx >= prod.getIngredients().size()) throw new IndexOutOfBoundsException();
                    prod.getIngredients().remove(idx);
                    plugin.getProductManager().save();
                    player.sendMessage(p() + "&aIngredient removed.");
                } catch (Exception e) { player.sendMessage(p() + "&cInvalid index."); }
            }

            case "addeffect" -> {
                Company c = requireCompany(player, Role.MANAGER); if (c == null) return true;
                if (args.length < 4) {
                    player.sendMessage(p() + "&cUsage: /product addeffect <id> <trigger> <type> [key=value ...]"); return true;
                }
                Product prod = resolveProduct(player, c, args[1]); if (prod == null) return true;
                int maxFx = plugin.getConfig().getInt("product-limits.max-effects", 5);
                if (prod.getEffects().size() >= maxFx) {
                    player.sendMessage(p() + "&cMax effects reached (" + maxFx + ")."); return true;
                }
                Trigger trigger;
                EffectType type;
                try { trigger = Trigger.valueOf(args[2].toUpperCase()); }
                catch (Exception e) { player.sendMessage(p() + "&cUnknown trigger. Valid: " + Arrays.toString(Trigger.values())); return true; }
                try { type = EffectType.valueOf(args[3].toUpperCase()); }
                catch (Exception e) { player.sendMessage(p() + "&cUnknown effect type. Valid: " + Arrays.toString(EffectType.values())); return true; }

                // Whitelist check
                if (!isWhitelisted(player, type, args)) return true;

                Map<String, Object> params = new LinkedHashMap<>();
                for (int i = 4; i < args.length; i++) {
                    String[] kv = args[i].split("=", 2);
                    if (kv.length == 2) params.put(kv[0], parseValue(kv[1]));
                }

                // Enforce limits
                if (params.containsKey("amplifier")) {
                    int maxAmp = plugin.getConfig().getInt("product-limits.max-amplifier", 3);
                    int amp = Integer.parseInt(params.get("amplifier").toString());
                    if (amp > maxAmp) { player.sendMessage(p() + "&cAmplifier too high. Max: " + maxAmp); return true; }
                }
                if (params.containsKey("duration_ticks")) {
                    int maxDur = plugin.getConfig().getInt("product-limits.max-duration-ticks", 2400);
                    int dur = Integer.parseInt(params.get("duration_ticks").toString());
                    if (dur > maxDur) { player.sendMessage(p() + "&cDuration too high. Max: " + maxDur + " ticks"); return true; }
                }

                prod.getEffects().add(new EffectModule(trigger, type, params));
                plugin.getProductManager().save();
                player.sendMessage(p() + "&aEffect added: &e" + trigger.name() + " → " + type.name());
            }

            case "removeeffect" -> {
                Company c = requireCompany(player, Role.MANAGER); if (c == null) return true;
                if (args.length < 3) { player.sendMessage(p() + "&cUsage: /product removeeffect <id> <index>"); return true; }
                Product prod = resolveProduct(player, c, args[1]); if (prod == null) return true;
                try {
                    int idx = Integer.parseInt(args[2]);
                    if (idx < 0 || idx >= prod.getEffects().size()) throw new IndexOutOfBoundsException();
                    prod.getEffects().remove(idx);
                    plugin.getProductManager().save();
                    player.sendMessage(p() + "&aEffect removed.");
                } catch (Exception e) { player.sendMessage(p() + "&cInvalid index."); }
            }

            case "setlore" -> {
                Company c = requireCompany(player, Role.MANAGER); if (c == null) return true;
                if (args.length < 4) { player.sendMessage(p() + "&cUsage: /product setlore <id> <line> <text>"); return true; }
                Product prod = resolveProduct(player, c, args[1]); if (prod == null) return true;
                try {
                    int line = Integer.parseInt(args[2]);
                    String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    List<String> lore = new ArrayList<>(prod.getLore());
                    while (lore.size() <= line) lore.add("");
                    lore.set(line, text);
                    prod.setLore(lore);
                    plugin.getProductManager().save();
                    player.sendMessage(p() + "&aLore line " + line + " set.");
                } catch (NumberFormatException e) { player.sendMessage(p() + "&cInvalid line number."); }
            }

            case "list" -> {
                Company c = requireCompany(player, Role.WORKER); if (c == null) return true;
                List<Product> products = plugin.getProductManager().getProductsForCompany(c.getId());
                if (products.isEmpty()) { player.sendMessage(p() + "&7No products registered."); return true; }
                player.sendMessage(p() + "&6--- " + c.getName() + " Products ---");
                for (Product prod : products) {
                    player.sendMessage("  &e" + prod.getId().toString().substring(0,8) + " &7| &f" + prod.getDisplayName()
                        + " &7| &f" + prod.getBaseItem().name());
                }
            }

            case "info" -> {
                Company c = requireCompany(player, Role.WORKER); if (c == null) return true;
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /product info <id>"); return true; }
                Product prod = resolveProduct(player, c, args[1]); if (prod == null) return true;
                player.sendMessage(p() + "&6--- " + prod.getDisplayName() + " ---");
                player.sendMessage("  &7Base: &f" + prod.getBaseItem().name());
                player.sendMessage("  &7Ingredients:");
                for (int i = 0; i < prod.getIngredients().size(); i++) {
                    Ingredient ing = prod.getIngredients().get(i);
                    player.sendMessage("    &7[" + i + "] &f" + ing.getAmount() + "x " + ing.getMaterial().name());
                }
                player.sendMessage("  &7Effects:");
                for (int i = 0; i < prod.getEffects().size(); i++) {
                    EffectModule em = prod.getEffects().get(i);
                    player.sendMessage("    &7[" + i + "] &e" + em.getTrigger().name() + " &7→ &f" + em.getType().name()
                        + " &8" + em.getParameters());
                }
            }

            case "delete" -> {
                Company c = requireCompany(player, Role.OWNER); if (c == null) return true;
                if (!c.getOwner().equals(player.getUniqueId())) { noPerms(player); return true; }
                if (args.length < 2) { player.sendMessage(p() + "&cUsage: /product delete <id>"); return true; }
                Product prod = resolveProduct(player, c, args[1]); if (prod == null) return true;
                plugin.getProductManager().deleteProduct(prod.getId());
                c.getProductIds().remove(prod.getId());
                plugin.getCompanyManager().save();
                player.sendMessage(p() + "&cProduct deleted.");
            }

            default -> sendHelp(player);
        }
        return true;
    }

    private boolean isWhitelisted(Player player, EffectType type, String[] args) {
        YamlConfiguration fx = plugin.getEffectsConfig();
        // check effect name if POTION_EFFECT
        for (int i = 4; i < args.length; i++) {
            String[] kv = args[i].split("=", 2);
            if (kv.length != 2) continue;
            if ("effect".equals(kv[0]) && type == EffectType.POTION_EFFECT) {
                if (!fx.getStringList("allowed-potion-effects").contains(kv[1].toUpperCase())) {
                    player.sendMessage(p() + "&cPotion effect &e" + kv[1] + " &cis not whitelisted.");
                    return false;
                }
            }
            if ("attribute".equals(kv[0]) && type == EffectType.ATTRIBUTE_MODIFIER) {
                if (!fx.getStringList("allowed-attributes").contains(kv[1].toUpperCase())) {
                    player.sendMessage(p() + "&cAttribute &e" + kv[1] + " &cis not whitelisted.");
                    return false;
                }
            }
            if ("particle".equals(kv[0]) && type == EffectType.PARTICLE_BURST) {
                if (!fx.getStringList("allowed-particles").contains(kv[1].toUpperCase())) {
                    player.sendMessage(p() + "&cParticle &e" + kv[1] + " &cis not whitelisted.");
                    return false;
                }
            }
            if ("sound".equals(kv[0]) && type == EffectType.SOUND_PLAY) {
                if (!fx.getStringList("allowed-sounds").contains(kv[1].toUpperCase())) {
                    player.sendMessage(p() + "&cSound &e" + kv[1] + " &cis not whitelisted.");
                    return false;
                }
            }
        }
        if (type == EffectType.COMMAND_RUN && !plugin.getConfig().getBoolean("product-limits.allow-command-effect", false)) {
            player.sendMessage(p() + "&cCOMMAND_RUN effects are disabled on this server.");
            return false;
        }
        return true;
    }

    private Object parseValue(String s) {
        try { return Integer.parseInt(s); } catch (Exception ignored) {}
        try { return Double.parseDouble(s); } catch (Exception ignored) {}
        if ("true".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s)) return false;
        return s;
    }

    private Product resolveProduct(Player player, Company c, String partialId) {
        for (UUID pid : c.getProductIds()) {
            Product p = plugin.getProductManager().getProduct(pid);
            if (p != null && p.getId().toString().startsWith(partialId.toLowerCase())) return p;
        }
        player.sendMessage(p() + "&cProduct not found in your company.");
        return null;
    }

    private Company requireCompany(Player p, Role minimum) {
        Company c = plugin.getCompanyManager().getCompanyOf(p.getUniqueId());
        if (c == null) { p.sendMessage(p() + plugin.getConfig().getString("messages.no-company","&cNo company.").replace('&','\u00A7')); return null; }
        if (!c.hasRole(p.getUniqueId(), minimum)) { noPerms(p); return null; }
        return c;
    }

    private void noPerms(Player p) { p.sendMessage(p() + "&cInsufficient role permissions."); }

    private void sendHelp(Player p) {
        p.sendMessage(p() + "&6--- Product Help ---");
        p.sendMessage("&e/product create <name>");
        p.sendMessage("&e/product setbase <id> <material>");
        p.sendMessage("&e/product addingredient <id> <material> <amount>");
        p.sendMessage("&e/product removeingredient <id> <index>");
        p.sendMessage("&e/product addeffect <id> <trigger> <type> [key=value...]");
        p.sendMessage("&e/product removeeffect <id> <index>");
        p.sendMessage("&e/product setlore <id> <line> <text>");
        p.sendMessage("&e/product list | info <id> | delete <id>");
    }

    private String p() { return plugin.getConfig().getString("messages.prefix","&8[&6angelCreate&8] &r").replace('&','\u00A7'); }
}
