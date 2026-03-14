package cn.popcraft.ipchecker.Services;

import cn.popcraft.ipchecker.IPChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.regex.Pattern;

public class BanService {

    private static final Pattern CIDR_PATTERN = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/\\d{1,2}$");
    private static final Pattern IP_PATTERN = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");

    private final IPChecker plugin;
    private final IPDatabaseService ipDatabaseService;

    public BanService(IPChecker plugin, IPDatabaseService ipDatabaseService) {
        this.plugin = plugin;
        this.ipDatabaseService = ipDatabaseService;
    }

    public void checkAndBan(Player player, String ip, boolean isDatacenter, boolean isVpn) {
        String reason = "";
        if (isDatacenter) {
            reason = "Datacenter IP";
        } else if (isVpn) {
            reason = "VPN IP";
        }

        if (!reason.isEmpty()) {
            long banExpireHours = plugin.getConfigManager().getBanExpireHours();
            long expireTime = banExpireHours > 0 ? System.currentTimeMillis() + (banExpireHours * 3600000) : 0;
            
            plugin.getStorageManager().addBan(ip, reason, expireTime, player.getName());
            
            String kickMessage = plugin.getConfigManager().getKickMessage();
            player.kickPlayer(kickMessage);

            String adminNotify = plugin.getConfigManager().getAdminNotifyMessage()
                    .replace("{player}", player.getName())
                    .replace("{type}", reason);
            
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("ipchecker.notify")) {
                    admin.sendMessage(adminNotify);
                }
            }

            if (plugin.getConfigManager().isLogEnabled()) {
                plugin.getLogger().warning("玩家 " + player.getName() + " (" + ip + ") 因 " + reason + " 被封禁");
            }
        }
    }

    public boolean unbanIP(String ip) {
        return plugin.getStorageManager().removeBan(ip);
    }

    public boolean isBanned(String ip) {
        return plugin.getStorageManager().isBanned(ip);
    }

    public String extractIP(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null) {
            plugin.getLogger().warning("无法获取玩家 " + player.getName() + " 的地址 (address is null)");
            return null;
        }
        InetAddress inetAddress = address.getAddress();
        if (inetAddress == null) {
            plugin.getLogger().warning("无法获取玩家 " + player.getName() + " 的 InetAddress");
            return null;
        }
        return inetAddress.getHostAddress();
    }

    public boolean isDatacenterIP(String ip) {
        return ipDatabaseService.isDatacenterIP(ip);
    }

    public boolean isVpnIP(String ip) {
        return ipDatabaseService.isVpnIP(ip);
    }

    public List<String> getBannedIPs() {
        return plugin.getStorageManager().getBannedIPs();
    }

    public List<String> getWhitelist() {
        return plugin.getStorageManager().getWhitelistedPlayers();
    }

    public void addToWhitelist(String playerName) {
        plugin.getStorageManager().addWhitelistedPlayer(playerName);
    }

    public boolean removeFromWhitelist(String playerName) {
        return plugin.getStorageManager().removeWhitelistedPlayer(playerName);
    }

    public boolean isWhitelisted(String playerName) {
        return plugin.getStorageManager().isPlayerWhitelisted(playerName);
    }

    public boolean isCIDR(String ip) {
        return CIDR_PATTERN.matcher(ip).matches();
    }

    public boolean isValidIP(String ip) {
        if (isCIDR(ip)) {
            String[] parts = ip.split("/");
            if (!isValidIPv4(parts[0])) {
                return false;
            }
            try {
                int prefix = Integer.parseInt(parts[1]);
                return prefix >= 0 && prefix <= 32;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return isValidIPv4(ip);
    }

    private boolean isValidIPv4(String ip) {
        if (!IP_PATTERN.matcher(ip).matches()) {
            return false;
        }
        String[] octets = ip.split("\\.");
        for (String octet : octets) {
            try {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
}
