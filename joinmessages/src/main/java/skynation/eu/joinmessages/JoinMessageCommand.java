package skynation.eu.joinmessages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinMessageCommand implements CommandExecutor {

    private final Joinmessages plugin;

    public JoinMessageCommand(Joinmessages plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("joinmessages.custom")) {
                plugin.openJoinMessageGUI(player);
            } else {
                player.sendMessage(plugin.getNoPermissionMessage());
            }
            return true;
        }
        sender.sendMessage("This command can only be used by players.");
        return false;
    }
}
