package cn.popcraft.ipchecker;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final IPChecker plugin;

    public ConfigManager(IPChecker plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public int getCheckDelay() {
        return getConfig().getInt("check-delay", 120);
    }

    public String getKickMessage() {
        return colorize(getConfig().getString("messages.kick", "&c检测到您的网络环境异常，请联系管理员"));
    }

    public String getAdminNotifyMessage() {
        return colorize(getConfig().getString("messages.admin-notify", "&c[IPChecker] 玩家 {player} 使用 VPN/机房 IP 已被封禁"));
    }

    public boolean isAutoUpdateEnabled() {
        return getConfig().getBoolean("auto-update.enabled", true);
    }

    public int getAutoUpdateInterval() {
        return getConfig().getInt("auto-update.interval-hours", 24);
    }

    public boolean isLogEnabled() {
        return getConfig().getBoolean("log-enabled", true);
    }

    private String colorize(String message) {
        if (message == null) return "";
        return message.replace("&", "§");
    }
}
