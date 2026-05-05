package me.angelique.angelCreate.managers;

import me.angelique.angelCreate.AngelCreate;
import me.angelique.angelCreate.models.EffectModule;
import me.angelique.angelCreate.models.enums.EffectType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import java.util.UUID;
import java.util.logging.Level;

public class EffectManager {

    private final AngelCreate plugin;

    public EffectManager(AngelCreate plugin) { this.plugin = plugin; }

    /**
     * Execute a single EffectModule for a given player (and optional target).
     */
    public void execute(EffectModule em, Player player, Entity target) {
        try {
            switch (em.getType()) {
                case POTION_EFFECT -> executePotionEffect(em, player, target);
                case DAMAGE_MODIFIER -> {} // handled inline in event, not here
                case ATTRIBUTE_MODIFIER -> executeAttributeModifier(em, player, true);
                case CHANCE_WRAPPER -> executeChanceWrapper(em, player, target);
                case LAUNCH_PROJECTILE -> executeLaunchProjectile(em, player);
                case PARTICLE_BURST -> executeParticleBurst(em, player);
                case SOUND_PLAY -> executeSoundPlay(em, player);
                case COMMAND_RUN -> executeCommandRun(em, player);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Effect execution error: " + em.getType(), e);
        }
    }

    public void removeAttributeModifier(EffectModule em, Player player) {
        if (em.getType() != EffectType.ATTRIBUTE_MODIFIER) return;
        executeAttributeModifier(em, player, false);
    }

    // ── POTION_EFFECT ─────────────────────────────────────────────────────────
    private void executePotionEffect(EffectModule em, Player player, Entity rawTarget) {
        String effectName = em.getParamString("effect", "SPEED");
        int amplifier = em.getParamInt("amplifier", 0);
        int durationTicks = em.getParamInt("duration_ticks", 200);
        String targetType = em.getParamString("target", "SELF");
        double radius = em.getParamDouble("area_radius", 5.0);

        PotionEffectType pet = PotionEffectType.getByName(effectName);
        if (pet == null) { plugin.getLogger().warning("Unknown potion effect: " + effectName); return; }

        PotionEffect effect = new PotionEffect(pet, durationTicks, amplifier);

        switch (targetType.toUpperCase()) {
            case "SELF" -> player.addPotionEffect(effect);
            case "TARGET" -> {
                if (rawTarget instanceof LivingEntity le) le.addPotionEffect(effect);
            }
            case "AREA" -> {
                for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
                    if (e instanceof LivingEntity le) le.addPotionEffect(effect);
                }
            }
        }
    }

    // ── ATTRIBUTE_MODIFIER ────────────────────────────────────────────────────
    @SuppressWarnings("deprecation")
    private void executeAttributeModifier(EffectModule em, Player player, boolean apply) {
        String attrName = em.getParamString("attribute", "GENERIC_MOVEMENT_SPEED");
        String opName = em.getParamString("operation", "ADD_NUMBER");
        double amount = em.getParamDouble("amount", 0.0);

        Attribute attribute;
        try { attribute = Attribute.valueOf(attrName); }
        catch (Exception e) { plugin.getLogger().warning("Unknown attribute: " + attrName); return; }

        AttributeModifier.Operation operation;
        try { operation = AttributeModifier.Operation.valueOf(opName); }
        catch (Exception e) { operation = AttributeModifier.Operation.ADD_NUMBER; }

        // Deterministic UUID so modifier is idempotent
        String productIdStr = em.getParamString("product_id", "unknown");
        UUID modUUID = UUID.nameUUIDFromBytes((productIdStr + attrName).getBytes());

        var inst = player.getAttribute(attribute);
        if (inst == null) return;

        // Remove existing first (idempotent)
        inst.getModifiers().stream()
            .filter(m -> m.getUniqueId().equals(modUUID))
            .findFirst().ifPresent(inst::removeModifier);

        if (apply) {
            AttributeModifier mod = new AttributeModifier(modUUID, "angelcreate_" + attrName, amount, operation);
            inst.addModifier(mod);
        }
    }

    // ── CHANCE_WRAPPER ────────────────────────────────────────────────────────
    private void executeChanceWrapper(EffectModule em, Player player, Entity target) {
        double chance = em.getParamDouble("chance", 1.0);
        if (Math.random() >= chance) return;
        Object wrapped = em.getParam("wrapped_effect");
        if (wrapped instanceof EffectModule nested) {
            execute(nested, player, target);
        }
    }

    // ── LAUNCH_PROJECTILE ─────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void executeLaunchProjectile(EffectModule em, Player player) {
        String projName = em.getParamString("projectile", "ARROW");
        double speed = em.getParamDouble("speed", 1.5);
        EntityType type;
        try { type = EntityType.valueOf(projName); }
        catch (Exception e) { plugin.getLogger().warning("Unknown projectile: " + projName); return; }

        if (!Projectile.class.isAssignableFrom(type.getEntityClass())) return;
        Vector dir = player.getLocation().getDirection().multiply(speed);
        player.launchProjectile((Class<? extends Projectile>) type.getEntityClass(), dir);
    }

    // ── PARTICLE_BURST ────────────────────────────────────────────────────────
    private void executeParticleBurst(EffectModule em, Player player) {
        String particleName = em.getParamString("particle", "FLAME");
        int count = em.getParamInt("count", 10);
        double radius = em.getParamDouble("radius", 1.5);
        Particle particle;
        try { particle = Particle.valueOf(particleName); }
        catch (Exception e) { plugin.getLogger().warning("Unknown particle: " + particleName); return; }
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count,
            radius, radius, radius, 0);
    }

    // ── SOUND_PLAY ────────────────────────────────────────────────────────────
    private void executeSoundPlay(EffectModule em, Player player) {
        String soundName = em.getParamString("sound", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) em.getParamDouble("volume", 1.0);
        float pitch = (float) em.getParamDouble("pitch", 1.0);
        Sound sound;
        try { sound = Sound.valueOf(soundName); }
        catch (Exception e) { plugin.getLogger().warning("Unknown sound: " + soundName); return; }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    // ── COMMAND_RUN ───────────────────────────────────────────────────────────
    private void executeCommandRun(EffectModule em, Player player) {
        if (!plugin.getConfig().getBoolean("product-limits.allow-command-effect", false)) return;
        String command = em.getParamString("command", "").replace("%player%", player.getName());
        String executor = em.getParamString("executor", "CONSOLE");
        if ("PLAYER".equalsIgnoreCase(executor)) {
            player.performCommand(command);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    /**
     * Apply DAMAGE_MODIFIER inline — returns modified damage value.
     * Called directly from EffectTriggerListener on damage events.
     */
    public double applyDamageModifier(EffectModule em, double baseDamage) {
        String op = em.getParamString("operation", "ADD");
        double amount = em.getParamDouble("amount", 0.0);
        return switch (op.toUpperCase()) {
            case "MULTIPLY" -> baseDamage * amount;
            default -> baseDamage + amount;
        };
    }
}
