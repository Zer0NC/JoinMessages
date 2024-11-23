package skynation.eu.joinmessages;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Joinmessages extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private ConfigManager configManager;
    private Map<UUID, UUID> firstJoinPlayers = new HashMap<>();
    private Map<UUID, Boolean> awaitingMessage = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        configManager = new ConfigManager(this);

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("joinmessage").setExecutor(new JoinMessageCommand(this));
        getCommand("joinmessage").setTabCompleter(new JoinMessageTabCompleter());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Berechtigungsprüfung für benutzerdefinierte Nachrichten
        if (!player.hasPermission("joinmessages.custom")) {
            if (configManager.getCustomJoinMessage(playerUUID) != null) {
                configManager.deleteCustomJoinMessage(playerUUID);
                String removalMessage = getFormattedMessage(
                        "messages.customMessageRemoved",
                        "&cDeine benutzerdefinierte Join-Nachricht wurde entfernt, da du keine Berechtigung mehr hast."
                );
                player.sendMessage(removalMessage);
            }
        }

        // Benutzerdefinierte Nachricht abrufen
        String customMessage = configManager.getCustomJoinMessage(playerUUID);
        String prefix = config.getString("prefix", "&7[Spieler] ");
        String customMessageFormat = config.getString("customJoinMessageFormat", "%prefix% %player% : %message%");

        if (customMessage != null && !customMessage.isEmpty()) {
            String formattedMessage = customMessageFormat
                    .replace("%prefix%", prefix)
                    .replace("%player%", player.getName())
                    .replace("%message%", customMessage);
            event.setJoinMessage(ChatColor.translateAlternateColorCodes('&', formattedMessage));
        } else if (config.getBoolean("enableJoinMessages")) {
            String defaultMessage = config.getString("joinMessage", "%prefix% &aWillkommen zurück auf dem Server, %player%!")
                    .replace("%prefix%", prefix)
                    .replace("%player%", player.getName());
            event.setJoinMessage(ChatColor.translateAlternateColorCodes('&', defaultMessage));
        } else {
            event.setJoinMessage(null);
        }

        if (!player.hasPlayedBefore() && config.getBoolean("enableFirstJoinMessage")) {
            String announcement = getFormattedMessage("firstJoinAnnouncement",
                    "Der Spieler %player% ist das erste Mal hier! Schreibe 'Hey %player%' um $100 zu erhalten.")
                    .replace("%player%", player.getName());
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', announcement));

            firstJoinPlayers.put(playerUUID, null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!config.getBoolean("enableLeaveMessages")) {
            event.setQuitMessage(null);
            return;
        }

        Player player = event.getPlayer();
        String prefix = config.getString("prefix", "&7[Spieler] ");
        String leaveMessage = config.getString("leaveMessage", "%prefix% &c%player% hat den Server verlassen.")
                .replace("%prefix%", prefix)
                .replace("%player%", player.getName());

        event.setQuitMessage(ChatColor.translateAlternateColorCodes('&', leaveMessage));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player greeter = event.getPlayer();
        String message = event.getMessage();

        if (awaitingMessage.containsKey(greeter.getUniqueId())) {
            awaitingMessage.remove(greeter.getUniqueId());
            configManager.setCustomJoinMessage(greeter.getUniqueId(), message);

            String customMessageSet = getFormattedMessage("messages.customMessageSet", "&aDeine benutzerdefinierte Join-Nachricht wurde festgelegt: &f%message%")
                    .replace("%message%", message);
            greeter.sendMessage(customMessageSet);
            event.setCancelled(true);
            return;
        }

        for (UUID newPlayerUUID : firstJoinPlayers.keySet()) {
            if (firstJoinPlayers.get(newPlayerUUID) == null) {
                Player newPlayer = Bukkit.getPlayer(newPlayerUUID);
                if (newPlayer != null && message.equalsIgnoreCase("Hey " + newPlayer.getName())) {
                    firstJoinPlayers.put(newPlayerUUID, greeter.getUniqueId());

                    String rewardCommand = config.getString("rewardCommand", "eco give %reward_player% 100")
                            .replace("%reward_player%", greeter.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand);

                    greeter.sendMessage(ChatColor.GREEN + "Du hast " + newPlayer.getName() + " begrüßt und eine Belohnung erhalten!");
                    break;
                }
            }
        }
    }

    public void openJoinMessageGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.GREEN + "Custom Join Message");

        ItemStack createMessage = new ItemStack(Material.PAPER);
        ItemMeta createMeta = createMessage.getItemMeta();
        createMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("gui.createMessageItem.name", "&eErstelle eine benutzerdefinierte Join-Nachricht")));

        List<String> createLore = new ArrayList<>();
        for (String line : config.getStringList("gui.createMessageItem.lore")) {
            createLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        createMeta.setLore(createLore);
        createMessage.setItemMeta(createMeta);

        ItemStack deleteMessage = new ItemStack(Material.BARRIER);
        ItemMeta deleteMeta = deleteMessage.getItemMeta();
        deleteMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("gui.deleteMessageItem.name", "&cLösche deine benutzerdefinierte Join-Nachricht")));

        List<String> deleteLore = new ArrayList<>();
        for (String line : config.getStringList("gui.deleteMessageItem.lore")) {
            deleteLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        deleteMeta.setLore(deleteLore);
        deleteMessage.setItemMeta(deleteMeta);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        gui.setItem(3, createMessage);
        gui.setItem(5, deleteMessage);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GREEN + "Custom Join Message")) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            if (clickedItem.getType() == Material.PAPER) {
                player.closeInventory();
                String enterMessage = getFormattedMessage("messages.enterCustomMessage", "&eGib jetzt deine benutzerdefinierte Join-Nachricht ein:");
                player.sendMessage(enterMessage);
                awaitingMessage.put(player.getUniqueId(), true);

            } else if (clickedItem.getType() == Material.BARRIER) {
                configManager.deleteCustomJoinMessage(player.getUniqueId());
                String deleteMessage = getFormattedMessage("messages.customMessageDeleted", "&cDeine benutzerdefinierte Join-Nachricht wurde gelöscht.");
                player.sendMessage(deleteMessage);
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (awaitingMessage.containsKey(event.getPlayer().getUniqueId())) {
            awaitingMessage.remove(event.getPlayer().getUniqueId());
            String cancelMessage = getFormattedMessage("messages.setCancelled", "&cDer Vorgang zum Setzen der Nachricht wurde abgebrochen.");
            event.getPlayer().sendMessage(cancelMessage);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public String getNoPermissionMessage() {
        return getFormattedMessage("messages.noPermission", "&cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
    }

    public String getFormattedMessage(String path, String defaultMessage) {
        String prefix = config.getString("prefix", "&7[System] ");
        String message = config.getString(path, defaultMessage);
        return ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
    }
}
