package cn.popcraft.ipchecker.Storage;

import cn.popcraft.ipchecker.IPChecker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class YamlStorage {

    private final IPChecker plugin;
    private final File bansFile;
    private FileConfiguration bansConfig;

    private final Map<String, BanEntry> bannedIPs = new HashMap<>();
    private final List<String> whitelistedIPs = new ArrayList<>();

    public static class BanEntry {
        private final String ip;
        private final String reason;
        private final long timestamp;

        public BanEntry(String ip, String reason, long timestamp) {
            this.ip = ip;
            this.reason = reason;
            this.timestamp = timestamp;
        }

        public String getIp() {
            return ip;
        }

        public String getReason() {
            return reason;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public YamlStorage(IPChecker plugin) {
        this.plugin = plugin;
        this.bansFile = new File(plugin.getDataFolder(), "bans.yml");
    }

    public void load() {
        if (!bansFile.exists()) {
            try {
                bansFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 bans.yml 文件: " + e.getMessage());
            }
        }

        bansConfig = YamlConfiguration.loadConfiguration(bansFile);
        
        bannedIPs.clear();
        ConfigurationSection bannedSection = bansConfig.getConfigurationSection("banned");
        if (bannedSection != null) {
            for (String ip : bannedSection.getKeys(false)) {
                String reason = bannedSection.getString(ip + ".reason", "Unknown");
                long timestamp = bannedSection.getLong(ip + ".time", System.currentTimeMillis());
                bannedIPs.put(ip, new BanEntry(ip, reason, timestamp));
            }
        }

        whitelistedIPs.clear();
        List<String> whitelist = bansConfig.getStringList("whitelisted");
        if (whitelist != null) {
            whitelistedIPs.addAll(whitelist);
        }

        plugin.getLogger().info("已加载 " + bannedIPs.size() + " 个封禁 IP 和 " + whitelistedIPs.size() + " 个白名单 IP");
    }

    public void save() {
        if (bansConfig == null) {
            bansConfig = new YamlConfiguration();
        }

        bansConfig.set("banned", null);
        for (BanEntry entry : bannedIPs.values()) {
            bansConfig.set("banned." + entry.getIp() + ".reason", entry.getReason());
            bansConfig.set("banned." + entry.getIp() + ".time", entry.getTimestamp());
        }

        bansConfig.set("whitelisted", whitelistedIPs);

        try {
            bansConfig.save(bansFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 bans.yml 文件: " + e.getMessage());
        }
    }

    public void addBan(String ip, String reason) {
        bannedIPs.put(ip, new BanEntry(ip, reason, System.currentTimeMillis()));
        save();
    }

    public boolean removeBan(String ip) {
        if (bannedIPs.remove(ip) != null) {
            save();
            return true;
        }
        return false;
    }

    public boolean isBanned(String ip) {
        return bannedIPs.containsKey(ip);
    }

    public BanEntry getBanEntry(String ip) {
        return bannedIPs.get(ip);
    }

    public Set<String> getBannedIPs() {
        return bannedIPs.keySet();
    }

    public Map<String, BanEntry> getAllBans() {
        return new HashMap<>(bannedIPs);
    }

    public void addWhitelist(String ip) {
        if (!whitelistedIPs.contains(ip)) {
            whitelistedIPs.add(ip);
            save();
        }
    }

    public boolean removeWhitelist(String ip) {
        if (whitelistedIPs.remove(ip)) {
            save();
            return true;
        }
        return false;
    }

    public boolean isWhitelisted(String ip) {
        return whitelistedIPs.contains(ip);
    }

    public List<String> getWhitelist() {
        return new ArrayList<>(whitelistedIPs);
    }
}
