package cn.popcraft.ipchecker.Storage;

import cn.popcraft.ipchecker.IPChecker;

import java.util.List;

public class StorageManager {

    private final IPChecker plugin;
    private final YamlStorage yamlStorage;
    private final SqliteStorage sqliteStorage;
    private final boolean useSqlite;

    public StorageManager(IPChecker plugin) {
        this.plugin = plugin;
        this.useSqlite = plugin.getConfigManager().isSqliteEnabled();
        this.yamlStorage = new YamlStorage(plugin);
        this.sqliteStorage = new SqliteStorage(plugin);
    }

    public void load() {
        if (useSqlite) {
            sqliteStorage.load();
            plugin.getLogger().info("使用 SQLite 存储");
        } else {
            yamlStorage.load();
            plugin.getLogger().info("使用 YAML 存储");
        }
    }

    public void save() {
        if (useSqlite) {
            sqliteStorage.save();
        } else {
            yamlStorage.save();
        }
    }

    public void close() {
        if (useSqlite) {
            sqliteStorage.close();
        }
    }

    public void addBan(String ip, String reason) {
        addBan(ip, reason, 0, null);
    }

    public void addBan(String ip, String reason, long expireTime, String playerName) {
        if (useSqlite) {
            sqliteStorage.addBan(ip, reason, expireTime, playerName);
        } else {
            yamlStorage.addBan(ip, reason);
        }
    }

    public boolean removeBan(String ip) {
        if (useSqlite) {
            return sqliteStorage.removeBan(ip);
        } else {
            return yamlStorage.removeBan(ip);
        }
    }

    public boolean isBanned(String ip) {
        if (useSqlite) {
            return sqliteStorage.isBanned(ip);
        } else {
            return yamlStorage.isBanned(ip);
        }
    }

    public Object getBanEntry(String ip) {
        if (useSqlite) {
            return sqliteStorage.getBanEntry(ip);
        } else {
            return yamlStorage.getBanEntry(ip);
        }
    }

    public List<String> getBannedIPs() {
        if (useSqlite) {
            return sqliteStorage.getBannedIPs();
        } else {
            return yamlStorage.getBannedIPs();
        }
    }

    public void addWhitelistedPlayer(String playerName) {
        if (useSqlite) {
            sqliteStorage.addWhitelistedPlayer(playerName);
        } else {
            yamlStorage.addWhitelistedPlayer(playerName);
        }
    }

    public boolean removeWhitelistedPlayer(String playerName) {
        if (useSqlite) {
            return sqliteStorage.removeWhitelistedPlayer(playerName);
        } else {
            return yamlStorage.removeWhitelistedPlayer(playerName);
        }
    }

    public boolean isPlayerWhitelisted(String playerName) {
        if (useSqlite) {
            return sqliteStorage.isPlayerWhitelisted(playerName);
        } else {
            return yamlStorage.isPlayerWhitelisted(playerName);
        }
    }

    public List<String> getWhitelistedPlayers() {
        if (useSqlite) {
            return sqliteStorage.getWhitelistedPlayers();
        } else {
            return yamlStorage.getWhitelistedPlayers();
        }
    }

    public void recordPlayerIP(String playerName, String ip) {
        if (useSqlite) {
            sqliteStorage.recordPlayerIP(playerName, ip);
        }
    }

    public List<String> getPlayerIPHistory(String playerName) {
        if (useSqlite) {
            return sqliteStorage.getPlayerIPHistory(playerName);
        }
        return List.of();
    }

    public void incrementStat(String key) {
        if (useSqlite) {
            sqliteStorage.incrementStat(key);
        }
    }

    public int getStat(String key) {
        if (useSqlite) {
            return sqliteStorage.getStat(key);
        }
        return 0;
    }

    public boolean isUsingSqlite() {
        return useSqlite;
    }
}
