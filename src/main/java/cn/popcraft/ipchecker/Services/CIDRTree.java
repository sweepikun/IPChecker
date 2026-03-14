package cn.popcraft.ipchecker.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CIDRTree {

    private static class Node {
        Node zero;
        Node one;
        boolean isEnd;
        int prefixLength;
    }

    private final Node root = new Node();
    private int size = 0;

    public void add(String cidr) {
        if (!cidr.contains("/")) {
            addIP(cidr);
            return;
        }

        String[] parts = cidr.split("/");
        String ip = parts[0];
        int prefixLen = Integer.parseInt(parts[1]);

        try {
            long ipLong = ipToLong(ip);
            insert(ipLong, prefixLen);
        } catch (Exception e) {
            // ignore invalid CIDR
        }
    }

    private void addIP(String ip) {
        try {
            long ipLong = ipToLong(ip);
            insert(ipLong, 32);
        } catch (Exception e) {
            // ignore invalid IP
        }
    }

    private void insert(long ipLong, int prefixLen) {
        Node current = root;
        for (int i = 31; i >= 32 - prefixLen; i--) {
            int bit = (int) ((ipLong >> i) & 1);
            if (bit == 0) {
                if (current.zero == null) {
                    current.zero = new Node();
                }
                current = current.zero;
            } else {
                if (current.one == null) {
                    current.one = new Node();
                }
                current = current.one;
            }
        }
        current.isEnd = true;
        current.prefixLength = prefixLen;
        size++;
    }

    public boolean contains(String ip) {
        try {
            long ipLong = ipToLong(ip);
            return search(ipLong);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean search(long ipLong) {
        Node current = root;
        for (int i = 31; i >= 0; i--) {
            if (current == null) return false;
            if (current.isEnd) return true;
            int bit = (int) ((ipLong >> i) & 1);
            if (bit == 0) {
                current = current.zero;
            } else {
                current = current.one;
            }
        }
        return current != null && current.isEnd;
    }

    public void addAll(Set<String> ips) {
        for (String entry : ips) {
            add(entry);
        }
    }

    public void clear() {
        root.zero = null;
        root.one = null;
        size = 0;
    }

    public int size() {
        return size;
    }

    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        return ((long) (Integer.parseInt(octets[0]) & 0xFF) << 24) |
                ((long) (Integer.parseInt(octets[1]) & 0xFF) << 16) |
                ((long) (Integer.parseInt(octets[2]) & 0xFF) << 8) |
                ((long) (Integer.parseInt(octets[3]) & 0xFF));
    }
}
