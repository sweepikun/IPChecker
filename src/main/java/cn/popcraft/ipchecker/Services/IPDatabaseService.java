package cn.popcraft.ipchecker.Services;

import cn.popcraft.ipchecker.IPChecker;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IPDatabaseService {

    private final IPChecker plugin;
    private final File ipFolder;
    private final File datacenterFile;
    private final File vpnFile;
    private final File hashFile;

    private final Set<String> datacenterIPs = new HashSet<>();
    private final Set<String> vpnIPs = new HashSet<>();

    private final CIDRTree datacenterTree = new CIDRTree();
    private final CIDRTree vpnTree = new CIDRTree();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private volatile boolean initialLoadDone = false;

    public IPDatabaseService(IPChecker plugin, File ipFolder) {
        this.plugin = plugin;
        this.ipFolder = ipFolder;
        this.datacenterFile = new File(ipFolder, "datacenter.txt");
        this.vpnFile = new File(ipFolder, "vpn.txt");
        this.hashFile = new File(ipFolder, "hash.txt");
    }

    public void load() {
        if (datacenterFile.exists() && vpnFile.exists()) {
            loadIPsFromFile(datacenterFile, datacenterIPs, "机房 IP");
            loadIPsFromFile(vpnFile, vpnIPs, "VPN IP");
            rebuildTrees();
            initialLoadDone = true;
            plugin.getLogger().info("IP 库加载完成：机房 IP " + datacenterIPs.size() + " 条，VPN IP " + vpnIPs.size() + " 条");
        } else {
            plugin.getLogger().info("IP 库文件不存在，将在后台下载...");
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            downloadFromSources();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                loadIPsFromFile(datacenterFile, datacenterIPs, "机房 IP");
                loadIPsFromFile(vpnFile, vpnIPs, "VPN IP");
                rebuildTrees();
                initialLoadDone = true;
                plugin.getLogger().info("后台 IP 库下载完成：机房 IP " + datacenterIPs.size() + " 条，VPN IP " + vpnIPs.size() + " 条");
            });
        });

        int interval = plugin.getConfigManager().getAutoUpdateInterval();
        if (plugin.getConfigManager().isAutoUpdateEnabled()) {
            scheduler.scheduleWithFixedDelay(this::checkAndUpdate, interval, interval, TimeUnit.HOURS);
            plugin.getLogger().info("已启动 IP 库自动更新，每 " + interval + " 小时检查一次");
        }
    }

    private void downloadFromSources() {
        List<String> datacenterUrls = plugin.getConfigManager().getIPDatabaseURLs("datacenter");
        List<String> vpnUrls = plugin.getConfigManager().getIPDatabaseURLs("vpn");

        if (!datacenterFile.exists()) {
            for (String url : datacenterUrls) {
                if (downloadFile(url, datacenterFile, "datacenter")) {
                    saveHashes();
                    break;
                }
            }
        }

        if (!vpnFile.exists()) {
            for (String url : vpnUrls) {
                if (downloadFile(url, vpnFile, "vpn")) {
                    saveHashes();
                    break;
                }
            }
        }
    }

    private void rebuildTrees() {
        datacenterTree.clear();
        datacenterTree.addAll(datacenterIPs);
        vpnTree.clear();
        vpnTree.addAll(vpnIPs);
    }

    private void checkAndUpdate() {
        plugin.getLogger().info("正在检查 IP 库更新...");
        downloadFromSources();
        loadIPsFromFile(datacenterFile, datacenterIPs, "机房 IP");
        loadIPsFromFile(vpnFile, vpnIPs, "VPN IP");
        rebuildTrees();
        saveHashes();
        plugin.getLogger().info("IP 库更新检查完成");
    }

    private boolean downloadFile(String urlString, File outputFile, String type) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            plugin.getLogger().info(type + " IP 库下载完成：" + outputFile.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("下载 " + type + " IP 库失败 (" + urlString + ")：" + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String calculateHash(File file) {
        if (!file.exists()) return "";
        try {
            long lastModified = file.lastModified();
            long size = file.length();
            return "file_" + lastModified + "_" + size;
        } catch (Exception e) {
            return "";
        }
    }

    private void saveHashes() {
        String hash = calculateHash(datacenterFile) + "|" + calculateHash(vpnFile);
        try {
            Files.writeString(hashFile.toPath(), hash, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("保存哈希值失败：" + e.getMessage());
        }
    }

    private void loadIPsFromFile(File file, Set<String> targetSet, String type) {
        targetSet.clear();
        if (!file.exists()) return;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    targetSet.add(line);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("加载 " + type + " 文件失败：" + e.getMessage());
        }
    }

    public Set<String> getDatacenterIPs() {
        return Collections.unmodifiableSet(new HashSet<>(datacenterIPs));
    }

    public Set<String> getVpnIPs() {
        return Collections.unmodifiableSet(new HashSet<>(vpnIPs));
    }

    public boolean isDatacenterIP(String ip) {
        return initialLoadDone && datacenterTree.contains(ip);
    }

    public boolean isVpnIP(String ip) {
        return initialLoadDone && vpnTree.contains(ip);
    }

    public boolean isInitialLoadDone() {
        return initialLoadDone;
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
