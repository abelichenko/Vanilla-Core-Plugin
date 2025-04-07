package ua.whoami.vanilla.utils;

import org.bukkit.entity.Player;
import ua.whoami.vanilla.core.CorePlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class PrivateMessageManager {
    private final CorePlugin plugin;
    private final HashMap<UUID, UUID> lastMessengers = new HashMap<>();
    private final HashSet<UUID> globalToggles = new HashSet<>();
    private final HashMap<UUID, HashSet<UUID>> playerToggles = new HashMap<>();

    public PrivateMessageManager(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void sendMessage(Player sender, Player receiver, String message) {
        lastMessengers.put(receiver.getUniqueId(), sender.getUniqueId());

        String senderMsg = plugin.getMessagesConfig().getMessage("pm-sent")
                .replace("%receiver%", receiver.getName())
                .replace("%message%", message);

        String receiverMsg = plugin.getMessagesConfig().getMessage("pm-received")
                .replace("%sender%", sender.getName())
                .replace("%message%", message);

        sender.sendMessage(senderMsg);
        receiver.sendMessage(receiverMsg);
    }

    public void reply(Player replier, String message) {
        UUID lastSender = lastMessengers.get(replier.getUniqueId());
        if (lastSender == null) {
            replier.sendMessage(plugin.getMessagesConfig().getMessage("no-reply-target"));
            return;
        }

        Player target = plugin.getServer().getPlayer(lastSender);
        if (target == null || !target.isOnline()) {
            replier.sendMessage(plugin.getMessagesConfig().getMessage("player-offline"));
            return;
        }

        sendMessage(replier, target, message);
    }

    public boolean toggleMessages(Player player, String targetName) {
        if (targetName == null || targetName.equalsIgnoreCase("всем")) {
            if (globalToggles.contains(player.getUniqueId())) {
                globalToggles.remove(player.getUniqueId());
                return false;
            } else {
                globalToggles.add(player.getUniqueId());
                return true;
            }
        } else {
            Player target = plugin.getServer().getPlayer(targetName);
            if (target == null) {
                player.sendMessage(plugin.getMessagesConfig().getMessage("player-not-found"));
                return false;
            }

            HashSet<UUID> toggles = playerToggles.computeIfAbsent(
                    player.getUniqueId(), k -> new HashSet<>());

            if (toggles.contains(target.getUniqueId())) {
                toggles.remove(target.getUniqueId());
                return false;
            } else {
                toggles.add(target.getUniqueId());
                return true;
            }
        }
    }

    public boolean canReceive(Player receiver, Player sender) {
        if (globalToggles.contains(receiver.getUniqueId())) {
            return false;
        }

        HashSet<UUID> toggles = playerToggles.get(receiver.getUniqueId());
        return toggles == null || !toggles.contains(sender.getUniqueId());
    }
}