package cn.popcraft.ipchecker.Services;

import cn.popcraft.ipchecker.IPChecker;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;

public class IPCheckerService {

    private final IPChecker plugin;
    private final IPDatabaseService ipDatabaseService;
    private final BanService banService;
    private final GeoIPService geoIPService;
    private final PlayerCheckCache playerCache;

    private final ConcurrentHashMap<String, Long> pendingChecks = new ConcurrentHashMap<>();

    public IPCheckerService(IPChecker plugin, IPDatabaseService ipDatabaseService, BanService banService, GeoIPService geoIPService) {
        this.plugin = plugin;
        this.ipDatabaseService = ipDatabaseService;
        this.banService = banService;
        this.geoIPService = geoIPService;
        this.playerCache = new PlayerCheckCache(plugin.getConfigManager().getPlayerCacheMinutes());
    }

    public void scheduleCheck(Player player) {
        String playerName = player.getName();
        
        if (playerCache.isChecked(playerName)) {
            if (!playerCache.isClean(playerName)) {
                player.kickPlayer(plugin.getConfigManager().getKickMessage());
            }
            return;
        }
        
        Long existingTaskId = pendingChecks.get(playerName);
        if (existingTaskId != null) {
            plugin.getServer().getScheduler().cancelTask(existingTaskId.intValue());
        }

        int delaySeconds = plugin.getConfigManager().getCheckDelay();
        long delayTicks = delaySeconds * 20L;

        long taskId = plugin.getServer().getScheduler().runTaskLaterAsynchronously(
                plugin,
                () -> performCheck(player),
                delayTicks
        ).getTaskId();

        pendingChecks.put(playerName, taskId);
    }

    private void performCheck(Player player) {
        String ip = banService.extractIP(player);
        if (ip == null) {
            return;
        }

        pendingChecks.remove(player.getName());

        if (!ipDatabaseService.isInitialLoadDone()) {
            playerCache.put(player.getName(), ip, true, "IP库未加载");
            return;
        }

        plugin.getStorageManager().recordPlayerIP(player.getName(), ip);
        plugin.getStorageManager().incrementStat("total_checks");

        if (banService.isBanned(ip)) {
            playerCache.put(player.getName(), ip, false, "Banned");
            String kickMessage = plugin.getConfigManager().getKickMessage();
            player.kickPlayer(kickMessage);
            return;
        }

        boolean isDatacenter = banService.isDatacenterIP(ip);
        boolean isVpn = banService.isVpnIP(ip);

        if (isDatacenter || isVpn) {
            playerCache.put(player.getName(), ip, false, isDatacenter ? "Datacenter" : "VPN");
            banService.checkAndBan(player, ip, isDatacenter, isVpn);
        } else {
            playerCache.put(player.getName(), ip, true, "Clean");
        }
    }

    public void cancelCheck(Player player) {
        Long taskId = pendingChecks.remove(player.getName());
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId.intValue());
        }
    }

    public boolean isPendingCheck(String playerName) {
        return pendingChecks.containsKey(playerName);
    }

    public PlayerCheckCache getPlayerCache() {
        return playerCache;
    }

    public GeoIPService getGeoIPService() {
        return geoIPService;
    }
}
