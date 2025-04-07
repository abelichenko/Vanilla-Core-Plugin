package ua.whoami.vanilla.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.whoami.vanilla.configs.MessagesConfig;
import ua.whoami.vanilla.core.CorePlugin;
import ua.whoami.vanilla.utils.PrivateMessageManager;

public class MessageCommands implements CommandExecutor {
    private final CorePlugin plugin;

    public MessageCommands(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessagesConfig().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        PrivateMessageManager pmManager = plugin.getPmManager();
        MessagesConfig messages = plugin.getMessagesConfig();

        try {
            switch (cmd.getName().toLowerCase()) {
                case "m":
                case "message":
                    if (!player.hasPermission("playerhomes.message")) {
                        player.sendMessage(messages.getMessage("no-permission"));
                        return true;
                    }

                    if (args.length < 2) {
                        player.sendMessage(messages.getMessage("message-usage"));
                        return true;
                    }

                    Player target = plugin.getServer().getPlayer(args[0]);
                    if (target == null || !target.isOnline()) {
                        player.sendMessage(messages.getMessage("player-offline"));
                        return true;
                    }

                    if (!pmManager.canReceive(target, player)) {
                        player.sendMessage(messages.getMessage("messages-disabled")
                                .replace("%player%", target.getName()));
                        return true;
                    }

                    String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                    pmManager.sendMessage(player, target, message);
                    return true;

                case "r":
                case "reply":
                    if (!player.hasPermission("playerhomes.message")) {
                        player.sendMessage(messages.getMessage("no-permission"));
                        return true;
                    }

                    if (args.length < 1) {
                        player.sendMessage(messages.getMessage("reply-usage"));
                        return true;
                    }

                    String replyMessage = String.join(" ", args);
                    pmManager.reply(player, replyMessage);
                    return true;

                case "mt":
                case "messagetoggle":
                    if (!player.hasPermission("playerhomes.messagetoggle")) {
                        player.sendMessage(messages.getMessage("no-permission"));
                        return true;
                    }

                    String targetName = args.length > 0 ? args[0] : "всем";
                    boolean isNowDisabled = pmManager.toggleMessages(player, targetName);

                    if (targetName.equalsIgnoreCase("всем")) {
                        String msg = isNowDisabled ?
                                messages.getMessage("global-messages-disabled") :
                                messages.getMessage("global-messages-enabled");
                        player.sendMessage(msg);
                    } else {
                        String msg = isNowDisabled ?
                                messages.getMessage("player-messages-disabled") :
                                messages.getMessage("player-messages-enabled");
                        player.sendMessage(msg.replace("%player%", targetName));
                    }
                    return true;
            }
        } catch (Exception e) {
            player.sendMessage(messages.getMessage("error-occurred"));
            plugin.getLogger().severe("Error in message command: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}