package cn.popcraft.ipchecker.Services;

import cn.popcraft.ipchecker.IPChecker;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IPDatabaseService {

    private static final String DATACENTER_URL = "https://cdn.jsdmirror.cn/gh/X4BNet/lists_vpn@main/output/datacenter/ipv4.txt";
    private static final String VPN_URL = "https://cdn.jsdmirror.cn/gh/X4BNet/lists_vpn@main/output/vpn/ipv4.txt";

    private final IPChecker plugin;
    private final File ipFolder;
    private final File datacenterFile;
    private final File vpnFile;
    private final File hashFile;

    private final Set<String> datacenterIPs = new HashSet<>();
    private final Set<String> vpnIPs = new HashSet<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public IPDatabaseService(IPChecker plugin, File ipFolder) {
        this.plugin = plugin;
        this.ipFolder = ipFolder;
        this.datacenterFile = new File(ipFolder, "datacenter.txt");
        this.vpnFile = new File(ipFolder, "vpn.txt");
        this.hashFile = new File(ipFolder, "hash.txt");
    }

    public void load() {
        downloadIfNeeded();
        
        int interval = plugin.getConfigManager().getAutoUpdateInterval();
        if (plugin.getConfigManager().isAutoUpdateEnabled()) {
            scheduler.scheduleWithFixedDelay(this::checkAndUpdate, interval, interval, TimeUnit.HOURS);
            plugin.getLogger().info("已启动 IP 库自动更新，每 " + interval + " 小时检查一次");
        }
        
        loadIPsFromFile(datacenterFile, datacenterIPs, "机房 IP");
        loadIPsFromFile(vpnFile, vpnIPs, "VPN IP");
        
        plugin.getLogger().info("IP 库加载完成：机房 IP " + datacenterIPs.size() + " 条，VPN IP " + vpnIPs.size() + " 条");
    }

    private void downloadIfNeeded() {
        if (!datacenterFile.exists() || !vpnFile.exists()) {
            plugin.getLogger().info("IP 库文件不存在，开始下载...");
            downloadFile(DATACENTER_URL, datacenterFile, "datacenter");
            downloadFile(VPN_URL, vpnFile, "vpn");
            saveHashes();
        } else {
            String localDatacenterHash = calculateHash(datacenterFile);
            String localVpnHash = calculateHash(vpnFile);
            String savedHashes = loadHashes();
            
            if (savedHashes != null) {
                String[] parts = savedHashes.split("\\|");
                if (parts.length == 2) {
                    if (!localDatacenterHash.equals(parts[0]) || !localVpnHash.equals(parts[1])) {
                        plugin.getLogger().info("检测到 IP 库可能已更新，重新下载...");
                        downloadFile(DATACENTER_URL, datacenterFile, "datacenter");
                        downloadFile(VPN_URL, vpnFile, "vpn");
                        saveHashes();
                    }
                }
            }
        }
    }

    private void checkAndUpdate() {
        plugin.getLogger().info("正在检查 IP 库更新...");
        
        String remoteDatacenterHash = getRemoteFileHash(DATACENTER_URL);
        String remoteVpnHash = getRemoteFileHash(VPN_URL);
        
        if (remoteDatacenterHash != null && !remoteDatacenterHash.equals(calculateHash(datacenterFile))) {
            plugin.getLogger().info("机房 IP 库有更新，正在下载...");
            downloadFile(DATACENTER_URL, datacenterFile, "datacenter");
            loadIPsFromFile(datacenterFile, datacenterIPs, "机房 IP");
        }
        
        if (remoteVpnHash != null && !remoteVpnHash.equals(calculateHash(vpnFile))) {
            plugin.getLogger().info("VPN IP 库有更新，正在下载...");
            downloadFile(VPN_URL, vpnFile, "vpn");
            loadIPsFromFile(vpnFile, vpnIPs, "VPN IP");
        }
        
        saveHashes();
        plugin.getLogger().info("IP 库更新检查完成");
    }

    private void downloadFile(String urlString, File outputFile, String type) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
            
            connection.disconnect();
            plugin.getLogger().info(type + " IP 库下载完成：" + outputFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("下载 " + type + " IP 库失败：" + e.getMessage());
        }
    }

    private String getRemoteFileHash(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(10000);
            
            long lastModified = connection.getLastModified();
            connection.disconnect();
            
            if (lastModified > 0) {
                return "lastmod_" + lastModified;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("获取远程文件信息失败：" + e.getMessage());
        }
        return null;
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

    private String loadHashes() {
        try {
            if (hashFile.exists()) {
                return Files.readString(hashFile.toPath(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("加载哈希值失败：" + e.getMessage());
        }
        return null;
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
        return datacenterIPs;
    }

    public Set<String> getVpnIPs() {
        return vpnIPs;
    }

    public boolean isDatacenterIP(String ip) {
        return datacenterIPs.contains(ip);
    }

    public boolean isVpnIP(String ip) {
        return vpnIPs.contains(ip);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
