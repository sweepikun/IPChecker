package cn.popcraft.ipchecker.Commands;

import cn.popcraft.ipchecker.IPChecker;
import cn.popcraft.ipchecker.Services.GeoIPService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InfoCommand implements CommandExecutor, TabCompleter {

    private final IPChecker plugin;

    public InfoCommand(IPChecker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ipchecker.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法：/ipinfo <玩家>");
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);
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
        sender.sendMessage(ChatColor.YELLOW + "白名单状态：" + (plugin.getStorageManager().isPlayerWhitelisted(player.getName()) ? ChatColor.GREEN + "是" : ChatColor.RED + "否"));
        sender.sendMessage(ChatColor.YELLOW + "机房 IP: " + (plugin.getBanService().isDatacenterIP(ip) ? ChatColor.RED + "是" : ChatColor.GREEN + "否"));
        sender.sendMessage(ChatColor.YELLOW + "VPN IP: " + (plugin.getBanService().isVpnIP(ip) ? ChatColor.RED + "是" : ChatColor.GREEN + "否"));

        if (plugin.getConfigManager().isShowGeoIP()) {
            GeoIPService.GeoIPResult geoResult = plugin.getIPCheckerService().getGeoIPService().lookup(ip);
            if (geoResult != null) {
                sender.sendMessage(ChatColor.YELLOW + "地理位置：" + ChatColor.WHITE + geoResult.getCountry() + ", " + geoResult.getCity());
                sender.sendMessage(ChatColor.YELLOW + "ISP: " + ChatColor.WHITE + geoResult.getIsp());
                sender.sendMessage(ChatColor.YELLOW + "代理/VPN: " + (geoResult.isProxy() ? ChatColor.RED + "是" : ChatColor.GREEN + "否"));
            }
        }

        if (plugin.getConfigManager().isSqliteEnabled()) {
            List<String> ipHistory = plugin.getStorageManager().getPlayerIPHistory(player.getName());
            if (!ipHistory.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "IP 历史记录：");
                for (String historyIP : ipHistory) {
                    sender.sendMessage(ChatColor.GRAY + "  " + historyIP);
                }
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
