package cn.popcraft.ipchecker.Services;

import cn.popcraft.ipchecker.IPChecker;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class IPCheckerService {

    private final IPChecker plugin;
    private final IPDatabaseService ipDatabaseService;
    private final BanService banService;

    private final ConcurrentHashMap<String, Long> pendingChecks = new ConcurrentHashMap<>();

    public IPCheckerService(IPChecker plugin, IPDatabaseService ipDatabaseService, BanService banService) {
        this.plugin = plugin;
        this.ipDatabaseService = ipDatabaseService;
        this.banService = banService;
    }

    public void scheduleCheck(Player player) {
        String playerName = player.getName();
        
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

        pendingChecks.put(playerName, (long) taskId);
    }

    private void performCheck(Player player) {
        String ip = banService.extractIP(player);
        if (ip == null) {
            return;
        }

        pendingChecks.remove(player.getName());

        if (banService.isBanned(ip)) {
            String kickMessage = plugin.getConfigManager().getKickMessage();
            player.kickPlayer(kickMessage);
            return;
        }

        boolean isDatacenter = banService.isDatacenterIP(ip);
        boolean isVpn = banService.isVpnIP(ip);

        if (isDatacenter || isVpn) {
            banService.checkAndBan(player, ip, isDatacenter, isVpn);
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
}
