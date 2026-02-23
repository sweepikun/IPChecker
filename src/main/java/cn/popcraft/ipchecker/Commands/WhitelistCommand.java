package cn.popcraft.ipchecker.Commands;

import cn.popcraft.ipchecker.IPChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

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
            sender.sendMessage(ChatColor.YELLOW + "用法：/ipwhitelist <add|remove|list> [IP]");
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
            sender.sendMessage(ChatColor.RED + "用法：/ipwhitelist add <IP>");
            return true;
        }

        String ip = args[1];
        if (!plugin.getBanService().isValidIP(ip)) {
            sender.sendMessage(ChatColor.RED + "无效的 IP 地址格式");
            return true;
        }

        plugin.getBanService().addToWhitelist(ip);
        sender.sendMessage(ChatColor.GREEN + "已将 " + ip + " 添加到白名单");
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法：/ipwhitelist remove <IP>");
            return true;
        }

        String ip = args[1];
        if (plugin.getBanService().removeFromWhitelist(ip)) {
            sender.sendMessage(ChatColor.GREEN + "已从白名单移除 " + ip);
        } else {
            sender.sendMessage(ChatColor.RED + "该 IP 不在白名单中");
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<String> whitelist = plugin.getBanService().getWhitelist();
        sender.sendMessage(ChatColor.YELLOW + "=== IP 白名单 ===");
        if (whitelist.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "白名单为空");
        } else {
            for (String ip : whitelist) {
                sender.sendMessage(ChatColor.WHITE + "  " + ip);
            }
        }
        sender.sendMessage(ChatColor.YELLOW + "共 " + whitelist.size() + " 个 IP");
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
                completions.addAll(plugin.getBanService().getWhitelist());
            }
        }

        String prefix = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix))
                .collect(java.util.stream.Collectors.toList());
    }
}
