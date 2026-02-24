package cn.popcraft.ipchecker.Storage;

import cn.popcraft.ipchecker.IPChecker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class YamlStorage {

    private final IPChecker plugin;
    private final File bansFile;
    private FileConfiguration bansConfig;

    private final Map<String, BanEntry> bannedIPs = new ConcurrentHashMap<>();
    private final List<String> whitelistedPlayers = Collections.synchronizedList(new ArrayList<>());

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

        whitelistedPlayers.clear();
        List<String> whitelist = bansConfig.getStringList("whitelisted-players");
        if (whitelist != null) {
            whitelistedPlayers.addAll(whitelist);
        }

        plugin.getLogger().info("已加载 " + bannedIPs.size() + " 个封禁 IP 和 " + whitelistedPlayers.size() + " 个白名单玩家");
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

        bansConfig.set("whitelisted-players", whitelistedPlayers);

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
        return Collections.unmodifiableSet(bannedIPs.keySet());
    }

    public Map<String, BanEntry> getAllBans() {
        return new HashMap<>(bannedIPs);
    }

    public void addWhitelistedPlayer(String playerName) {
        String name = playerName.toLowerCase();
        if (!whitelistedPlayers.contains(name)) {
            whitelistedPlayers.add(name);
            save();
        }
    }

    public boolean removeWhitelistedPlayer(String playerName) {
        String name = playerName.toLowerCase();
        if (whitelistedPlayers.remove(name)) {
            save();
            return true;
        }
        return false;
    }

    public boolean isPlayerWhitelisted(String playerName) {
        return whitelistedPlayers.contains(playerName.toLowerCase());
    }

    public List<String> getWhitelistedPlayers() {
        return new ArrayList<>(whitelistedPlayers);
    }
}
