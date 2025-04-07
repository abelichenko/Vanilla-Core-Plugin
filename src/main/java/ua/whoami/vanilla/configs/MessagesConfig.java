package ua.whoami.vanilla.configs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class MessagesConfig {
    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private File messagesFile;

    public MessagesConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
    }

    public void loadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
            plugin.getLogger().log(Level.INFO, "Created messages.yml file");
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);

        try (InputStream is = plugin.getResource("messages.yml");
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
            messages.setDefaults(defConfig);
            messages.options().copyDefaults(true);
            saveMessages();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load messages.yml", e);
        }
    }

    public String getMessage(String key) {
        if (!messages.contains(key)) {
            plugin.getLogger().warning("Message key not found: " + key);
            return "§c[Message error: " + key + " not configured]";
        }

        String message = messages.getString(key);
        if (message == null) {
            return "§c[Null message for key: " + key + "]";
        }
        return message.replace('&', '§');
    }

    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save messages.yml", e);
        }
    }
}