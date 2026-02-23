package cn.popcraft.ipchecker;

import cn.popcraft.ipchecker.Commands.InfoCommand;
import cn.popcraft.ipchecker.Commands.ReloadCommand;
import cn.popcraft.ipchecker.Commands.UnbanCommand;
import cn.popcraft.ipchecker.Commands.WhitelistCommand;
import cn.popcraft.ipchecker.Listeners.PlayerJoinListener;
import cn.popcraft.ipchecker.Services.BanService;
import cn.popcraft.ipchecker.Services.IPCheckerService;
import cn.popcraft.ipchecker.Services.IPDatabaseService;
import cn.popcraft.ipchecker.Storage.YamlStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class IPChecker extends JavaPlugin {

    private static IPChecker instance;
    private ConfigManager configManager;
    private YamlStorage yamlStorage;
    private BanService banService;
    private IPDatabaseService ipDatabaseService;
    private IPCheckerService ipCheckerService;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        
        File dataFolder = new File(getDataFolder(), "ips");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        yamlStorage = new YamlStorage(this);
        yamlStorage.load();
        
        banService = new BanService(this, yamlStorage);
        
        ipDatabaseService = new IPDatabaseService(this, dataFolder);
        ipDatabaseService.load();
        
        ipCheckerService = new IPCheckerService(this, ipDatabaseService, banService);
        
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        
        registerCommands();
        
        getLogger().info("IPChecker has been enabled!");
    }

    @Override
    public void onDisable() {
        if (yamlStorage != null) {
            yamlStorage.save();
        }
        getLogger().info("IPChecker has been disabled!");
    }

    private void registerCommands() {
        getCommand("ipchecker").setExecutor(new UnbanCommand(this));
        getCommand("ipchecker").setTabCompleter(new UnbanCommand(this));
        
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

    public YamlStorage getYamlStorage() {
        return yamlStorage;
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
