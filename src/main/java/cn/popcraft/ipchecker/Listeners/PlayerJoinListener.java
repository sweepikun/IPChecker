package cn.popcraft.ipchecker.Listeners;

import cn.popcraft.ipchecker.IPChecker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final IPChecker plugin;

    public PlayerJoinListener(IPChecker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (player.hasPermission("ipchecker.bypass")) {
            return;
        }

        if (plugin.getYamlStorage().isPlayerWhitelisted(player.getName())) {
            return;
        }

        plugin.getIPCheckerService().scheduleCheck(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getIPCheckerService().cancelCheck(player);
    }
}
