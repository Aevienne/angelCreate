package me.angelique.angelCreate.listeners;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.EffectModule;
import me.angelique.angelCreate.models.Product;
import me.angelique.angelCreate.models.enums.EffectType;
import me.angelique.angelCreate.models.enums.Trigger;
import me.angelique.angelCreate.util.PDCUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class EffectTriggerListener implements Listener {

    private final AngelCreate plugin;
    private final NamespacedKey productIdKey;
    private final NamespacedKey companyIdKey;

    public EffectTriggerListener(AngelCreate plugin) {
        this.plugin = plugin;
        this.productIdKey = new NamespacedKey(plugin, "product_id");
        this.companyIdKey = new NamespacedKey(plugin, "company_id");
    }

    // ── ON_CONSUME ────────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        Product p = getProduct(e.getItem());
        if (p == null) return;
        fireEffects(p, Trigger.ON_CONSUME, e.getPlayer(), null);
    }

    // ── ON_HIT / ON_RECEIVE_HIT ───────────────────────────────────────────────
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        // ON_HIT — attacker is player
        if (e.getDamager() instanceof Player attacker) {
            ItemStack hand = attacker.getInventory().getItemInMainHand();
            Product p = getProduct(hand);
            if (p != null) {
                // DAMAGE_MODIFIER inline
                for (EffectModule em : getEffectsForTrigger(p, Trigger.ON_HIT)) {
                    if (em.getType() == EffectType.DAMAGE_MODIFIER) {
                        String target = em.getParamString("target", "TARGET");
                        if ("TARGET".equalsIgnoreCase(target)) {
                            e.setDamage(plugin.getEffectManager().applyDamageModifier(em, e.getDamage()));
                        }
                    } else {
                        plugin.getEffectManager().execute(em, attacker, e.getEntity());
                    }
                }
            }
        }

        // ON_RECEIVE_HIT — victim is player
        if (e.getEntity() instanceof Player victim) {
            // check mainhand + all armor slots
            ItemStack[] toCheck = {
                victim.getInventory().getItemInMainHand(),
                victim.getInventory().getHelmet(),
                victim.getInventory().getChestplate(),
                victim.getInventory().getLeggings(),
                victim.getInventory().getBoots()
            };
            for (ItemStack item : toCheck) {
                Product p = getProduct(item);
                if (p == null) continue;
                for (EffectModule em : getEffectsForTrigger(p, Trigger.ON_RECEIVE_HIT)) {
                    if (em.getType() == EffectType.DAMAGE_MODIFIER) {
                        String target = em.getParamString("target", "SELF");
                        if ("SELF".equalsIgnoreCase(target)) {
                            e.setDamage(plugin.getEffectManager().applyDamageModifier(em, e.getDamage()));
                        }
                    } else {
                        plugin.getEffectManager().execute(em, victim, e.getDamager());
                    }
                }
            }
        }
    }

    // ── ON_EQUIP / ON_UNEQUIP ─────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorChange(PlayerArmorChangeEvent e) {
        Player player = e.getPlayer();

        // UNEQUIP old
        Product oldP = getProduct(e.getOldItem());
        if (oldP != null) {
            for (EffectModule em : getEffectsForTrigger(oldP, Trigger.ON_UNEQUIP)) {
                if (em.getType() == EffectType.ATTRIBUTE_MODIFIER) {
                    plugin.getEffectManager().removeAttributeModifier(em, player);
                } else {
                    plugin.getEffectManager().execute(em, player, null);
                }
            }
        }

        // EQUIP new
        Product newP = getProduct(e.getNewItem());
        if (newP != null) {
            for (EffectModule em : getEffectsForTrigger(newP, Trigger.ON_EQUIP)) {
                plugin.getEffectManager().execute(em, player, null);
            }
        }
    }

    // ── ON_SNEAK ──────────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        Product p = getProduct(e.getPlayer().getInventory().getItemInMainHand());
        if (p == null) return;
        fireEffects(p, Trigger.ON_SNEAK, e.getPlayer(), null);
    }

    // ── ON_INTERACT ───────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() == null) return;
        switch (e.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {}
            default -> { return; }
        }
        Product p = getProduct(e.getItem());
        if (p == null) return;
        fireEffects(p, Trigger.ON_INTERACT, e.getPlayer(), null);
    }

    // ── helpers ───────────────────────────────────────────────────────────────
    private Product getProduct(ItemStack item) {
        if (item == null) return null;
        String pidStr = PDCUtil.getString(item, productIdKey);
        if (pidStr == null) return null;
        try { return plugin.getProductManager().getProduct(UUID.fromString(pidStr)); }
        catch (Exception ex) { return null; }
    }

    private List<EffectModule> getEffectsForTrigger(Product p, Trigger trigger) {
        return p.getEffects().stream().filter(e -> e.getTrigger() == trigger).toList();
    }

    private void fireEffects(Product p, Trigger trigger, Player player, Entity target) {
        for (EffectModule em : getEffectsForTrigger(p, trigger)) {
            plugin.getEffectManager().execute(em, player, target);
        }
    }
}
