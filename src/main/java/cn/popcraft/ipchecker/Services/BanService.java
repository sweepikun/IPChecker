package cn.popcraft.ipchecker.Services;

import cn.popcraft.ipchecker.IPChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
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
            plugin.getYamlStorage().addBan(ip, reason);
            
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
        return plugin.getYamlStorage().removeBan(ip);
    }

    public boolean isBanned(String ip) {
        if (plugin.getYamlStorage().isBanned(ip)) {
            return true;
        }

        for (String bannedIP : plugin.getYamlStorage().getBannedIPs()) {
            if (isCIDR(bannedIP) && matchCIDR(ip, bannedIP)) {
                return true;
            }
        }

        return false;
    }

    public boolean matchCIDR(String ip, String cidr) {
        if (!isCIDR(cidr)) {
            return ip.equals(cidr);
        }

        try {
            String[] parts = cidr.split("/");
            String cidrIP = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            InetAddress targetAddr = InetAddress.getByName(ip);
            InetAddress cidrAddr = InetAddress.getByName(cidrIP);

            byte[] targetBytes = targetAddr.getAddress();
            byte[] cidrBytes = cidrAddr.getAddress();

            if (targetBytes.length != cidrBytes.length) {
                return false;
            }

            long mask = 0xFFFFFFFFL << (32 - prefixLength);
            long targetInt = ((targetBytes[0] & 0xFFL) << 24) |
                             ((targetBytes[1] & 0xFFL) << 16) |
                             ((targetBytes[2] & 0xFFL) << 8) |
                             (targetBytes[3] & 0xFFL);
            long cidrInt = ((cidrBytes[0] & 0xFFL) << 24) |
                           ((cidrBytes[1] & 0xFFL) << 16) |
                           ((cidrBytes[2] & 0xFFL) << 8) |
                           (cidrBytes[3] & 0xFFL);

            return (targetInt & mask) == (cidrInt & mask);
        } catch (Exception e) {
            return false;
        }
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
        if (ipDatabaseService.isDatacenterIP(ip)) {
            return true;
        }

        for (String cidr : getBannedCIDRs(ipDatabaseService.getDatacenterIPs())) {
            if (matchCIDR(ip, cidr)) {
                return true;
            }
        }

        return false;
    }

    public boolean isVpnIP(String ip) {
        if (ipDatabaseService.isVpnIP(ip)) {
            return true;
        }

        for (String cidr : getBannedCIDRs(ipDatabaseService.getVpnIPs())) {
            if (matchCIDR(ip, cidr)) {
                return true;
            }
        }

        return false;
    }

    private List<String> getBannedCIDRs(java.util.Set<String> ips) {
        List<String> cidrs = new ArrayList<>();
        for (String ip : ips) {
            if (isCIDR(ip)) {
                cidrs.add(ip);
            }
        }
        return cidrs;
    }

    public List<String> getBannedIPs() {
        return new ArrayList<>(plugin.getYamlStorage().getBannedIPs());
    }

    public List<String> getWhitelist() {
        return plugin.getYamlStorage().getWhitelistedPlayers();
    }

    public void addToWhitelist(String playerName) {
        plugin.getYamlStorage().addWhitelistedPlayer(playerName);
    }

    public boolean removeFromWhitelist(String playerName) {
        return plugin.getYamlStorage().removeWhitelistedPlayer(playerName);
    }

    public boolean isWhitelisted(String playerName) {
        return plugin.getYamlStorage().isPlayerWhitelisted(playerName);
    }
}
