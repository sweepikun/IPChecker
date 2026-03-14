package cn.popcraft.ipchecker;

import cn.popcraft.ipchecker.Commands.InfoCommand;
import cn.popcraft.ipchecker.Commands.ReloadCommand;
import cn.popcraft.ipchecker.Commands.UnbanCommand;
import cn.popcraft.ipchecker.Commands.WhitelistCommand;
import cn.popcraft.ipchecker.Listeners.PlayerJoinListener;
import cn.popcraft.ipchecker.Services.BanService;
import cn.popcraft.ipchecker.Services.GeoIPService;
import cn.popcraft.ipchecker.Services.IPCheckerService;
import cn.popcraft.ipchecker.Services.IPDatabaseService;
import cn.popcraft.ipchecker.Storage.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class IPChecker extends JavaPlugin {

    private static IPChecker instance;
    private ConfigManager configManager;
    private StorageManager storageManager;
    private BanService banService;
    private IPDatabaseService ipDatabaseService;
    private IPCheckerService ipCheckerService;
    private GeoIPService geoIPService;
    private ConfigWatcher configWatcher;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        
        File dataFolder = new File(getDataFolder(), "ips");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        storageManager = new StorageManager(this);
        storageManager.load();

        ipDatabaseService = new IPDatabaseService(this, dataFolder);
        ipDatabaseService.load();

        banService = new BanService(this, ipDatabaseService);
        geoIPService = new GeoIPService(this);
        
        ipCheckerService = new IPCheckerService(this, ipDatabaseService, banService, geoIPService);
        
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        
        registerCommands();
        
        if (configManager.isConfigHotReload()) {
            configWatcher = new ConfigWatcher(this);
            configWatcher.start();
            getLogger().info("配置热重载已启用");
        }
        
        getLogger().info("IPChecker has been enabled!");
    }

    @Override
    public void onDisable() {
        if (configWatcher != null) {
            configWatcher.stop();
        }
        if (storageManager != null) {
            storageManager.close();
        }
        if (ipDatabaseService != null) {
            ipDatabaseService.shutdown();
        }
        getLogger().info("IPChecker has been disabled!");
    }

    private void registerCommands() {
        UnbanCommand unbanCommand = new UnbanCommand(this);
        getCommand("ipchecker").setExecutor(unbanCommand);
        getCommand("ipchecker").setTabCompleter(unbanCommand);

        InfoCommand infoCommand = new InfoCommand(this);
        getCommand("ipinfo").setExecutor(infoCommand);
        getCommand("ipinfo").setTabCompleter(infoCommand);

        getCommand("ipreload").setExecutor(new ReloadCommand(this));

        WhitelistCommand whitelistCommand = new WhitelistCommand(this);
        getCommand("ipwhitelist").setExecutor(whitelistCommand);
        getCommand("ipwhitelist").setTabCompleter(whitelistCommand);
    }

    public static IPChecker getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public BanService getBanService() {
        return banService;
    }

    public IPDatabaseService getIPDatabaseService() {
        return ipDatabaseService;
    }

    public IPCheckerService getIPCheckerService() {
        return ipCheckerService;
    }
}
