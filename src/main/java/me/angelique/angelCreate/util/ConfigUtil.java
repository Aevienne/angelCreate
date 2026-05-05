package me.angelique.angelCreate.util;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigUtil {

    public static double getDouble(FileConfiguration config, String path, double def) {
        return config.isSet(path) ? config.getDouble(path) : def;
    }

    public static int getInt(FileConfiguration config, String path, int def) {
        return config.isSet(path) ? config.getInt(path) : def;
    }

    public static String getString(FileConfiguration config, String path, String def) {
        return config.isSet(path) ? config.getString(path, def) : def;
    }

    public static boolean getBool(FileConfiguration config, String path, boolean def) {
        return config.isSet(path) ? config.getBoolean(path) : def;
    }
}
