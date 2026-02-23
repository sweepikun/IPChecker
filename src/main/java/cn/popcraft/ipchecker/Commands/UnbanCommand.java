package cn.popcraft.ipchecker.Commands;

import cn.popcraft.ipchecker.IPChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UnbanCommand implements CommandExecutor, TabCompleter {

    private final IPChecker plugin;

    public UnbanCommand(IPChecker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ipchecker.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "用法：/ipchecker <unban|info|reload|whitelist> [参数]");
            sender.sendMessage(ChatColor.YELLOW + "  unban <IP> - 解封指定 IP");
            sender.sendMessage(ChatColor.YELLOW + "  info <玩家> - 查看玩家 IP 信息");
            sender.sendMessage(ChatColor.YELLOW + "  reload - 重载配置");
            sender.sendMessage(ChatColor.YELLOW + "  whitelist <add|remove> <IP> - 管理白名单");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "unban":
                return handleUnban(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "reload":
                return handleReload(sender);
            case "whitelist":
                return handleWhitelist(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "未知的子命令：" + subCommand);
                return true;
        }
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法：/ipchecker unban <IP>");
            return true;
        }

        String ip = args[1];
        if (!plugin.getBanService().isValidIP(ip)) {
            sender.sendMessage(ChatColor.RED + "无效的 IP 地址格式");
            return true;
        }

        if (plugin.getBanService().unbanIP(ip)) {
            sender.sendMessage(ChatColor.GREEN + "已成功解封 IP: " + ip);
        } else {
            sender.sendMessage(ChatColor.RED + "该 IP 未被封禁");
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法：/ipchecker info <玩家>");
            return true;
        }

        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "玩家不在线");
            return true;
        }

        String ip = plugin.getBanService().extractIP(player);
        if (ip == null) {
            sender.sendMessage(ChatColor.RED + "无法获取玩家 IP");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "=== 玩家 IP 信息 ===");
        sender.sendMessage(ChatColor.YELLOW + "玩家：" + ChatColor.WHITE + player.getName());
        sender.sendMessage(ChatColor.YELLOW + "IP: " + ChatColor.WHITE + ip);
        sender.sendMessage(ChatColor.YELLOW + "封禁状态：" + (plugin.getBanService().isBanned(ip) ? ChatColor.RED + "已封禁" : ChatColor.GREEN + "正常"));
        sender.sendMessage(ChatColor.YELLOW + "白名单状态：" + (plugin.getBanService().isWhitelisted(ip) ? ChatColor.GREEN + "是" : ChatColor.RED + "否"));
        sender.sendMessage(ChatColor.YELLOW + "机房 IP: " + (plugin.getBanService().isDatacenterIP(ip) ? ChatColor.RED + "是" : ChatColor.GREEN + "否"));
        sender.sendMessage(ChatColor.YELLOW + "VPN IP: " + (plugin.getBanService().isVpnIP(ip) ? ChatColor.RED + "是" : ChatColor.GREEN + "否"));

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        plugin.getYamlStorage().load();
        sender.sendMessage(ChatColor.GREEN + "配置已重载");
        return true;
    }

    private boolean handleWhitelist(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法：/ipchecker whitelist <add|remove> <IP>");
            return true;
        }

        String action = args[1].toLowerCase();
        String ip = args[2];

        if (!plugin.getBanService().isValidIP(ip)) {
            sender.sendMessage(ChatColor.RED + "无效的 IP 地址格式");
            return true;
        }

        switch (action) {
            case "add":
                plugin.getBanService().addToWhitelist(ip);
                sender.sendMessage(ChatColor.GREEN + "已将 " + ip + " 添加到白名单");
                break;
            case "remove":
                if (plugin.getBanService().removeFromWhitelist(ip)) {
                    sender.sendMessage(ChatColor.GREEN + "已从白名单移除 " + ip);
                } else {
                    sender.sendMessage(ChatColor.RED + "该 IP 不在白名单中");
                }
                break;
            default:
                sender.sendMessage(ChatColor.RED + "未知操作：" + action);
                return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("unban");
            completions.add("info");
            completions.add("reload");
            completions.add("whitelist");
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("unban")) {
                completions.addAll(plugin.getBanService().getBannedIPs());
            } else if (subCommand.equals("info")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()));
            } else if (subCommand.equals("whitelist")) {
                completions.add("add");
                completions.add("remove");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("whitelist")) {
            completions.addAll(plugin.getBanService().getWhitelist());
        }

        String prefix = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
    }
}
