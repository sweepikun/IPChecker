package cn.popcraft.ipchecker.Commands;

import cn.popcraft.ipchecker.IPChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WhitelistCommand implements CommandExecutor, TabCompleter {

    private final IPChecker plugin;

    public WhitelistCommand(IPChecker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ipchecker.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "用法：/ipwhitelist <add|remove|list> [玩家]");
            sender.sendMessage(ChatColor.GRAY + "  add <玩家> - 将玩家加入白名单（跳过IP检查）");
            sender.sendMessage(ChatColor.GRAY + "  remove <玩家> - 将玩家从白名单移除");
            sender.sendMessage(ChatColor.GRAY + "  list - 查看白名单玩家列表");
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "add":
                return handleAdd(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender);
            default:
                sender.sendMessage(ChatColor.RED + "未知操作：" + action);
                return true;
        }
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法：/ipwhitelist add <玩家>");
            return true;
        }

        String playerName = args[1];
        plugin.getYamlStorage().addWhitelistedPlayer(playerName);
        sender.sendMessage(ChatColor.GREEN + "已将玩家 " + playerName + " 添加到白名单（该玩家将跳过IP检查）");
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法：/ipwhitelist remove <玩家>");
            return true;
        }

        String playerName = args[1];
        if (plugin.getYamlStorage().removeWhitelistedPlayer(playerName)) {
            sender.sendMessage(ChatColor.GREEN + "已从白名单移除玩家 " + playerName);
        } else {
            sender.sendMessage(ChatColor.RED + "该玩家不在白名单中");
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<String> whitelist = plugin.getYamlStorage().getWhitelistedPlayers();
        sender.sendMessage(ChatColor.YELLOW + "=== IP 检查白名单 ===");
        if (whitelist.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "白名单为空");
        } else {
            for (String player : whitelist) {
                sender.sendMessage(ChatColor.WHITE + "  " + player);
            }
        }
        sender.sendMessage(ChatColor.YELLOW + "共 " + whitelist.size() + " 个玩家");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("add");
            completions.add("remove");
            completions.add("list");
        } else if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (action.equals("remove") || action.equals("add")) {
                String prefix = args[1].toLowerCase();
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(p -> p.getName())
                        .filter(name -> name.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList()));
            }
        }

        return completions;
    }
}
