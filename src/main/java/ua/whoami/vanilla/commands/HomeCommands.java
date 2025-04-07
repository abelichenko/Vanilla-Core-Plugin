package ua.whoami.vanilla.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import java.util.UUID;
import java.util.Map;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import ua.whoami.vanilla.configs.MessagesConfig;
import ua.whoami.vanilla.core.CorePlugin;
import ua.whoami.vanilla.utils.DatabaseManager;

public class HomeCommands implements CommandExecutor {
    private CorePlugin plugin;

    public HomeCommands(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessagesConfig().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();
        DatabaseManager dbManager = plugin.getDatabaseManager();
        MessagesConfig messages = plugin.getMessagesConfig();

        try {
            if (cmd.getName().equalsIgnoreCase("sethome")) {
                if (!player.hasPermission("playerhomes.sethome")) {
                    player.sendMessage(messages.getMessage("no-permission"));
                    return true;
                }

                if (args.length == 0) {
                    player.sendMessage(messages.getMessage("sethome-usage"));
                    return true;
                }

                String homeName = args[0].toLowerCase();
                if (homeName.length() > 16) {
                    player.sendMessage(messages.getMessage("name-too-long"));
                    return true;
                }

                int maxHomes = getMaxHomes(player);
                int currentHomes = dbManager.getHomeCount(playerUuid);

                // Проверяем, не существует ли уже дом с таким именем
                if (dbManager.getHome(playerUuid, homeName) != null) {
                    // Обновляем существующий дом
                    dbManager.setHome(playerUuid, homeName, player.getLocation());
                    player.sendMessage(messages.getMessage("home-updated").replace("%home%", homeName));
                    return true;
                }

                // Проверяем лимит домов
                if (currentHomes >= maxHomes) {
                    player.sendMessage(messages.getMessage("home-limit").replace("%max%", String.valueOf(maxHomes)));
                    return true;
                }

                // Создаем новый дом
                if (dbManager.setHome(playerUuid, homeName, player.getLocation())) {
                    player.sendMessage(messages.getMessage("home-set").replace("%home%", homeName));
                } else {
                    player.sendMessage(messages.getMessage("home-set-failed"));
                }

            } else if (cmd.getName().equalsIgnoreCase("delhome")) {
                if (!player.hasPermission("playerhomes.delhome")) {
                    player.sendMessage(messages.getMessage("no-permission"));
                    return true;
                }

                if (args.length == 0) {
                    player.sendMessage(messages.getMessage("delhome-usage"));
                    return true;
                }

                String homeName = args[0].toLowerCase();
                if (dbManager.deleteHome(playerUuid, homeName)) {
                    player.sendMessage(messages.getMessage("home-deleted").replace("%home%", homeName));
                } else {
                    player.sendMessage(messages.getMessage("home-not-found").replace("%home%", homeName));
                }

            } else if (cmd.getName().equalsIgnoreCase("home")) {
                if (!player.hasPermission("playerhomes.home")) {
                    player.sendMessage(messages.getMessage("no-permission"));
                    return true;
                }

                Map<String, Location> homes = dbManager.getHomes(playerUuid);

                if (args.length == 0) {
                    if (homes.isEmpty()) {
                        player.sendMessage(messages.getMessage("no-homes"));
                    } else {
                        if (homes.size() == 1) {
                            String homeName = homes.keySet().iterator().next();
                            teleportToHome(player, homeName, homes.get(homeName));
                        } else {
                            player.sendMessage(messages.getMessage("home-list"));
                            for (String name : homes.keySet()) {
                                player.sendMessage(" - " + name);
                            }
                        }
                    }
                    return true;
                }

                String homeName = args[0].toLowerCase();
                Location homeLocation = homes.get(homeName);

                if (homeLocation == null) {
                    player.sendMessage(messages.getMessage("home-not-found").replace("%home%", homeName));
                    return true;
                }

                teleportToHome(player, homeName, homeLocation);
            }
        } catch (Exception e) {
            player.sendMessage(messages.getMessage("error-occurred"));
            plugin.getLogger().severe("Error executing command: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private void teleportToHome(Player player, String homeName, Location location) {
        player.teleport(location);
        player.sendMessage(plugin.getMessagesConfig().getMessage("teleported-to-home")
                .replace("%home%", homeName));
    }

    private int getMaxHomes(Player player) {
        int defaultHomes = plugin.getPluginConfig().getInt("default-max-homes", 3);

        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            int maxHomes = luckPerms.getUserManager().getUser(player.getUniqueId())
                    .getCachedData().getPermissionData()
                    .getPermissionMap().entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("playerhomes.maxhomes."))
                    .filter(entry -> entry.getValue())
                    .mapToInt(entry -> {
                        try {
                            return Integer.parseInt(entry.getKey().substring("playerhomes.maxhomes.".length()));
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .max()
                    .orElse(defaultHomes);

            return Math.max(maxHomes, defaultHomes);
        } catch (IllegalStateException e) {
            return defaultHomes;
        }
    }
}