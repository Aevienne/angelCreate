package me.angelique.angelCreate.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class PDCUtil {

    public static final String NAMESPACE = "angelcreate";

    public static NamespacedKey key(Plugin plugin, String key) {
        return new NamespacedKey(plugin, key);
    }

    public static void setString(ItemStack item, NamespacedKey key, String value) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    public static String getString(ItemStack item, NamespacedKey key) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static boolean hasKey(ItemStack item, NamespacedKey key) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public static boolean isAngelCreateItem(ItemStack item, NamespacedKey productIdKey) {
        return hasKey(item, productIdKey);
    }
}
