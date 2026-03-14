package cn.popcraft.ipchecker;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ConfigWatcher {

    private final IPChecker plugin;
    private final File configFile;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong lastModified = new AtomicLong(0);
    private volatile boolean watching = false;

    public ConfigWatcher(IPChecker plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void start() {
        if (configFile.exists()) {
            lastModified.set(configFile.lastModified());
        }

        watching = true;
        scheduler.scheduleWithFixedDelay(() -> {
            if (!watching) return;
            try {
                if (configFile.exists()) {
                    long currentModified = configFile.lastModified();
                    if (currentModified > lastModified.getAndSet(currentModified)) {
                        plugin.getLogger().info("检测到 config.yml 已修改，自动重载配置...");
                        plugin.getConfigManager().reload();
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("配置热重载检查失败：" + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        watching = false;
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
