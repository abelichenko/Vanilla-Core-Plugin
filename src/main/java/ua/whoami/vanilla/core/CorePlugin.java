package ua.whoami.vanilla.core;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.logging.Logger;
import ua.whoami.vanilla.commands.HomeCommands;
import ua.whoami.vanilla.commands.MessageCommands;
import ua.whoami.vanilla.configs.MessagesConfig;
import ua.whoami.vanilla.utils.DatabaseManager;
import ua.whoami.vanilla.utils.PrivateMessageManager;

public class CorePlugin extends JavaPlugin {
    private DatabaseManager dbManager;
    private MessagesConfig messagesConfig;
    private FileConfiguration config;
    private Logger logger;
    private PrivateMessageManager pmManager;

    @Override
    public void onEnable() {
        logger = getLogger();

        //                                 Загрузка конфигурации
        saveDefaultConfig();
        config = getConfig();

        //                                   Загрузка сообщений
        messagesConfig = new MessagesConfig(this);
        messagesConfig.loadMessages();

        //                                 Инициализация базы данных
        dbManager = new DatabaseManager(this);
        dbManager.initializeDatabase();

        //                              Инициализация приватных сообщений
        pmManager = new PrivateMessageManager(this);

        //                                    Регистрация команд для домиков
        getCommand("sethome").setExecutor(new HomeCommands(this));
        getCommand("delhome").setExecutor(new HomeCommands(this));
        getCommand("home").setExecutor(new HomeCommands(this));

        //                                    Регистрация команд для ЛС
        getCommand("m").setExecutor(new MessageCommands(this));
        getCommand("message").setExecutor(new MessageCommands(this));
        getCommand("r").setExecutor(new MessageCommands(this));
        getCommand("reply").setExecutor(new MessageCommands(this));
        getCommand("mt").setExecutor(new MessageCommands(this));
        getCommand("messagetoggle").setExecutor(new MessageCommands(this));

        logger.info("VanillaCore plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (dbManager != null) {
            dbManager.closeConnection();
        }
        logger.info("VanillaCore plugin has been disabled!");
    }

    public PrivateMessageManager getPmManager() {
        return pmManager;
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }
}