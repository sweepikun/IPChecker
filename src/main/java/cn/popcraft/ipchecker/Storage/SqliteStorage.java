package cn.popcraft.ipchecker.Storage;

import cn.popcraft.ipchecker.IPChecker;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SqliteStorage {

    private final IPChecker plugin;
    private final File dbFile;
    private Connection connection;

    private final ConcurrentHashMap<String, BanEntry> banCache = new ConcurrentHashMap<>();
    private final List<String> whitelistCache = new ArrayList<>();

    public static class BanEntry {
        private final String ip;
        private final String reason;
        private final long timestamp;
        private final long expireTime;
        private final String playerName;

        public BanEntry(String ip, String reason, long timestamp, long expireTime, String playerName) {
            this.ip = ip;
            this.reason = reason;
            this.timestamp = timestamp;
            this.expireTime = expireTime;
            this.playerName = playerName;
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

        public long getExpireTime() {
            return expireTime;
        }

        public boolean isExpired() {
            return expireTime > 0 && System.currentTimeMillis() > expireTime;
        }

        public String getPlayerName() {
            return playerName;
        }
    }

    public SqliteStorage(IPChecker plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "data.db");
    }

    public void load() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            loadBans();
            loadWhitelist();
            cleanupExpiredBans();
            plugin.getLogger().info("SQLite 存储加载完成：" + banCache.size() + " 个封禁，" + whitelistCache.size() + " 个白名单玩家");
        } catch (Exception e) {
            plugin.getLogger().severe("无法初始化 SQLite 数据库：" + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS bans (" +
                    "ip TEXT PRIMARY KEY," +
                    "reason TEXT," +
                    "timestamp INTEGER," +
                    "expire_time INTEGER DEFAULT 0," +
                    "player_name TEXT" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS whitelist (" +
                    "player_name TEXT PRIMARY KEY" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS player_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_name TEXT," +
                    "ip TEXT," +
                    "first_seen INTEGER," +
                    "last_seen INTEGER," +
                    "UNIQUE(player_name, ip)" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS stats (" +
                    "key TEXT PRIMARY KEY," +
                    "value INTEGER DEFAULT 0" +
                    ")");
            stmt.execute("INSERT OR IGNORE INTO stats (key, value) VALUES ('total_bans', 0)");
            stmt.execute("INSERT OR IGNORE INTO stats (key, value) VALUES ('total_checks', 0)");
        }
    }

    private void loadBans() {
        banCache.clear();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT ip, reason, timestamp, expire_time, player_name FROM bans")) {
            while (rs.next()) {
                String ip = rs.getString("ip");
                String reason = rs.getString("reason");
                long timestamp = rs.getLong("timestamp");
                long expireTime = rs.getLong("expire_time");
                String playerName = rs.getString("player_name");
                banCache.put(ip, new BanEntry(ip, reason, timestamp, expireTime, playerName));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载封禁数据失败：" + e.getMessage());
        }
    }

    private void loadWhitelist() {
        whitelistCache.clear();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_name FROM whitelist")) {
            while (rs.next()) {
                whitelistCache.add(rs.getString("player_name").toLowerCase());
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载白名单数据失败：" + e.getMessage());
        }
    }

    public void save() {
        // SQLite is already persisted, nothing to do
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("关闭数据库连接失败：" + e.getMessage());
            }
        }
    }

    public void addBan(String ip, String reason, long expireTime, String playerName) {
        banCache.put(ip, new BanEntry(ip, reason, System.currentTimeMillis(), expireTime, playerName));
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO bans (ip, reason, timestamp, expire_time, player_name) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, ip);
            ps.setString(2, reason);
            ps.setLong(3, System.currentTimeMillis());
            ps.setLong(4, expireTime);
            ps.setString(5, playerName);
            ps.executeUpdate();
            incrementStat("total_bans");
        } catch (SQLException e) {
            plugin.getLogger().severe("添加封禁失败：" + e.getMessage());
        }
    }

    public boolean removeBan(String ip) {
        banCache.remove(ip);
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM bans WHERE ip = ?")) {
            ps.setString(1, ip);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("移除封禁失败：" + e.getMessage());
            return false;
        }
    }

    public boolean isBanned(String ip) {
        BanEntry entry = banCache.get(ip);
        if (entry == null) return false;
        if (entry.isExpired()) {
            removeBan(ip);
            return false;
        }
        return true;
    }

    public BanEntry getBanEntry(String ip) {
        BanEntry entry = banCache.get(ip);
        if (entry != null && entry.isExpired()) {
            removeBan(ip);
            return null;
        }
        return entry;
    }

    public List<String> getBannedIPs() {
        return new ArrayList<>(banCache.keySet());
    }

    public void addWhitelistedPlayer(String playerName) {
        String name = playerName.toLowerCase();
        if (!whitelistCache.contains(name)) {
            whitelistCache.add(name);
            try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO whitelist (player_name) VALUES (?)")) {
                ps.setString(1, name);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("添加白名单失败：" + e.getMessage());
            }
        }
    }

    public boolean removeWhitelistedPlayer(String playerName) {
        String name = playerName.toLowerCase();
        if (whitelistCache.remove(name)) {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM whitelist WHERE player_name = ?")) {
                ps.setString(1, name);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("移除白名单失败：" + e.getMessage());
            }
        }
        return false;
    }

    public boolean isPlayerWhitelisted(String playerName) {
        return whitelistCache.contains(playerName.toLowerCase());
    }

    public List<String> getWhitelistedPlayers() {
        return new ArrayList<>(whitelistCache);
    }

    public void recordPlayerIP(String playerName, String ip) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_history (player_name, ip, first_seen, last_seen) VALUES (?, ?, ?, ?) " +
                        "ON CONFLICT(player_name, ip) DO UPDATE SET last_seen = ?")) {
            long now = System.currentTimeMillis();
            ps.setString(1, playerName);
            ps.setString(2, ip);
            ps.setLong(3, now);
            ps.setLong(4, now);
            ps.setLong(5, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("记录玩家 IP 失败：" + e.getMessage());
        }
    }

    public List<String> getPlayerIPHistory(String playerName) {
        List<String> ips = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ip, first_seen, last_seen FROM player_history WHERE player_name = ? ORDER BY last_seen DESC")) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ips.add(rs.getString("ip"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取玩家 IP 历史失败：" + e.getMessage());
        }
        return ips;
    }

    public void incrementStat(String key) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE stats SET value = value + 1 WHERE key = ?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            // ignore
        }
    }

    public int getStat(String key) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM stats WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("value");
                }
            }
        } catch (SQLException e) {
            // ignore
        }
        return 0;
    }

    private void cleanupExpiredBans() {
        int count = 0;
        try (Statement stmt = connection.createStatement()) {
            count = stmt.executeUpdate("DELETE FROM bans WHERE expire_time > 0 AND expire_time < " + System.currentTimeMillis());
        } catch (SQLException e) {
            plugin.getLogger().warning("清理过期封禁失败：" + e.getMessage());
        }
        if (count > 0) {
            plugin.getLogger().info("已清理 " + count + " 个过期封禁");
            loadBans();
        }
    }
}
