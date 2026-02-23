package cn.popcraft.ipchecker.Commands;

import cn.popcraft.ipchecker.IPChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final IPChecker plugin;

    public ReloadCommand(IPChecker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ipchecker.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return true;
        }

        plugin.getConfigManager().reload();
        plugin.getYamlStorage().load();
        sender.sendMessage(ChatColor.GREEN + "配置已重载");
        return true;
    }
}
