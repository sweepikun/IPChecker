package cn.popcraft.ipchecker;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final IPChecker plugin;
    private FileConfiguration config;

    public ConfigManager(IPChecker plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public int getCheckDelay() {
        return config.getInt("check-delay", 120);
    }

    public String getKickMessage() {
        return colorize(config.getString("messages.kick", "&c检测到您的网络环境异常，请联系管理员"));
    }

    public String getAdminNotifyMessage() {
        return colorize(config.getString("messages.admin-notify", "&c[IPChecker] 玩家 {player} 使用 VPN/机房 IP 已被封禁"));
    }

    public boolean isAutoUpdateEnabled() {
        return config.getBoolean("auto-update.enabled", true);
    }

    public int getAutoUpdateInterval() {
        return config.getInt("auto-update.interval-hours", 24);
    }

    public boolean isLogEnabled() {
        return config.getBoolean("log-enabled", true);
    }

    private String colorize(String message) {
        if (message == null) return "";
        return message.replace("&", "§");
    }
}
