package skynation.eu.joinmessages;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ConfigManager {

    private final JavaPlugin plugin;
    private File customConfigFile;
    private FileConfiguration customConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createCustomConfig();
    }

    private void createCustomConfig() {
        customConfigFile = new File(plugin.getDataFolder(), "joinmessages.yml");
        if (!customConfigFile.exists()) {
            try {
                customConfigFile.getParentFile().mkdirs();  // Verzeichnisse erstellen, falls nicht vorhanden
                customConfigFile.createNewFile();           // Datei erstellen, falls sie nicht existiert
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    }

    public FileConfiguration getConfig() {
        return this.customConfig;
    }

    public void saveConfig() {
        try {
            customConfig.save(customConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCustomJoinMessage(UUID playerUUID, String message) {
        customConfig.set("customMessages." + playerUUID.toString(), message);
        saveConfig();
    }

    public String getCustomJoinMessage(UUID playerUUID) {
        return customConfig.getString("customMessages." + playerUUID.toString());
    }

    public void deleteCustomJoinMessage(UUID playerUUID) {
        customConfig.set("customMessages." + playerUUID.toString(), null);
        saveConfig();
    }
}
