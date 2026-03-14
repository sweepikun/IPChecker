package cn.popcraft.ipchecker.Services;

import cn.popcraft.ipchecker.IPChecker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class GeoIPService {

    private final IPChecker plugin;
    private final ConcurrentHashMap<String, GeoIPResult> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 3600000; // 1 hour

    public static class GeoIPResult {
        private final String ip;
        private final String country;
        private final String city;
        private final String isp;
        private final boolean isProxy;
        private final long timestamp;

        public GeoIPResult(String ip, String country, String city, String isp, boolean isProxy) {
            this.ip = ip;
            this.country = country;
            this.city = city;
            this.isp = isp;
            this.isProxy = isProxy;
            this.timestamp = System.currentTimeMillis();
        }

        public String getIp() {
            return ip;
        }

        public String getCountry() {
            return country;
        }

        public String getCity() {
            return city;
        }

        public String getIsp() {
            return isp;
        }

        public boolean isProxy() {
            return isProxy;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }

    public GeoIPService(IPChecker plugin) {
        this.plugin = plugin;
    }

    public GeoIPResult lookup(String ip) {
        GeoIPResult cached = cache.get(ip);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }

        try {
            String urlStr = "http://ip-api.com/json/" + ip + "?fields=status,message,country,city,isp,proxy&lang=zh-CN";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            conn.disconnect();

            String json = response.toString();
            if (json.contains("\"status\":\"success\"")) {
                String country = extractField(json, "country");
                String city = extractField(json, "city");
                String isp = extractField(json, "isp");
                boolean isProxy = json.contains("\"proxy\":true");

                GeoIPResult result = new GeoIPResult(ip, country, city, isp, isProxy);
                cache.put(ip, result);
                return result;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("GeoIP 查询失败 (" + ip + "): " + e.getMessage());
        }

        return null;
    }

    public GeoIPResult lookupAsync(String ip) {
        GeoIPResult cached = cache.get(ip);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> lookup(ip));
        return null;
    }

    private String extractField(String json, String field) {
        String search = "\"" + field + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "Unknown";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "Unknown";
        return json.substring(start, end);
    }

    public void clearCache() {
        cache.clear();
    }
}
