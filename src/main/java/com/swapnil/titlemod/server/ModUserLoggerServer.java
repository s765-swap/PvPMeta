package com.swapnil.titlemod.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class ModUserLoggerServer {
    private static final int PORT = 5050;
    private static final String REQUIRED_MOD_VERSION = "pvpmeta-beta1";
    private static final int MAX_ENTRIES_PER_FILE = 1000;
    private static final String DATA_DIR = "moduserlog";
    private static final long DATA_EXPIRY_MS = 24 * 60 * 60 * 1000L;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private static final Object fileLock = new Object();
    private static int currentFileIndex = 1;
    private static int currentFileEntries = 0;
    private static File currentFile;
    private static final String SUSPECTED_ALTS_FILE = DATA_DIR + "/suspectedalters.json";
    private static final Map<String, Set<String>> ipToNames = new ConcurrentHashMap<>();
    private static final Object suspectedAltsLock = new Object();

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(DATA_DIR));
        rotateFileIfNeeded();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(ModUserLoggerServer::deleteOldDataFiles, 1, 1, TimeUnit.HOURS);
        System.out.println("[ModUserLoggerServer] Listening on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client)).start();
            }
        }
    }

    private static void handleClient(Socket client) {
        String clientIp = client.getInetAddress().getHostAddress();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String line = in.readLine();
            // Accept both: EVENT::NAME::MOD_VERSION and EVENT::NAME::MOD_VERSION::IP
            if (line == null || !line.contains("::")) {
                out.println("ERROR: Invalid format. Expected: EVENT::NAME::MOD_VERSION or EVENT::NAME::MOD_VERSION::IP");
                return;
            }
            String[] parts = line.split("::");
            if (parts.length != 3 && parts.length != 4) {
                out.println("ERROR: Invalid format. Expected: EVENT::NAME::MOD_VERSION or EVENT::NAME::MOD_VERSION::IP");
                return;
            }
            String event = parts[0].trim();
            String name = parts[1].trim();
            String modVersion = parts[2].trim();
            String ip = (parts.length == 4) ? parts[3].trim() : clientIp;
            if (!event.equals("JOIN") && !event.equals("QUIT")) {
                out.println("ERROR: EVENT must be JOIN or QUIT");
                return;
            }
            if (!modVersion.equals(REQUIRED_MOD_VERSION)) {
                out.println("POPUP: Please upgrade your mod to '" + REQUIRED_MOD_VERSION + "' or contact staff.");
                return;
            }
            if (event.equals("JOIN")) {
                onlineUsers.add(name);
                addIpToName(ip, name, modVersion);
                checkForAlts(ip, name, modVersion);
            } else {
                onlineUsers.remove(name);
            }
            logEvent(event, name, ip, modVersion);
            System.out.println("[ModUserLoggerServer] " + name + " " + event + " (" + onlineUsers.size() + " players with mod online)");
            out.println("OK");
        } catch (IOException e) {
            System.err.println("[ModUserLoggerServer] Error: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private static void logEvent(String event, String name, String ip, String modVersion) {
        synchronized (fileLock) {
            try {
                rotateFileIfNeeded();
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("timestamp", DATE_FORMAT.format(new Date()));
                entry.put("event", event);
                entry.put("name", name);
                entry.put("ip", ip);
                entry.put("modVersion", modVersion);
                String json = toJson(entry);
                try (FileWriter fw = new FileWriter(currentFile, true)) {
                    fw.write(json + "\n");
                }
                currentFileEntries++;
            } catch (IOException e) {
                System.err.println("[ModUserLoggerServer] Failed to log event: " + e.getMessage());
            }
        }
    }

    private static void rotateFileIfNeeded() throws IOException {
        if (currentFile == null || currentFileEntries >= MAX_ENTRIES_PER_FILE) {
            currentFile = new File(DATA_DIR, "data" + currentFileIndex + ".json");
            while (currentFile.exists() && countLines(currentFile) >= MAX_ENTRIES_PER_FILE) {
                currentFileIndex++;
                currentFile = new File(DATA_DIR, "data" + currentFileIndex + ".json");
            }
            currentFileEntries = (currentFile.exists() ? countLines(currentFile) : 0);
        }
    }

    private static int countLines(File file) throws IOException {
        if (!file.exists()) return 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int lines = 0;
            while (reader.readLine() != null) lines++;
            return lines;
        }
    }

    private static void deleteOldDataFiles() {
        File dir = new File(DATA_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("data") && name.endsWith(".json"));
        if (files == null) return;
        long now = System.currentTimeMillis();
        for (File file : files) {
            if (now - file.lastModified() > DATA_EXPIRY_MS) {
                if (file.delete()) {
                    System.out.println("[ModUserLoggerServer] Deleted old data file: " + file.getName());
                }
            }
        }
    }

    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append('"').append(e.getKey()).append('"').append(":");
            Object v = e.getValue();
            if (v instanceof String) {
                sb.append('"').append(((String) v).replace("\"", "\\\"")).append('"');
            } else {
                sb.append(v);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static void addIpToName(String ip, String name, String modVersion) {
        ipToNames.compute(ip, (k, v) -> {
            if (v == null) v = new HashSet<>();
            v.add(name);
            return v;
        });
    }

    private static void checkForAlts(String ip, String name, String modVersion) {
        Set<String> names = ipToNames.get(ip);
        if (names != null && names.size() > 1) {
            synchronized (suspectedAltsLock) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("timestamp", DATE_FORMAT.format(new Date()));
                entry.put("ip", ip);
                entry.put("modVersion", modVersion);
                entry.put("names", new ArrayList<>(names));
                appendToSuspectedAlts(entry);
            }
        }
    }

    private static void appendToSuspectedAlts(Map<String, Object> entry) {
        try (FileWriter fw = new FileWriter(SUSPECTED_ALTS_FILE, true)) {
            fw.write(toJson(entry) + "\n");
        } catch (IOException e) {
            System.err.println("[ModUserLoggerServer] Failed to log suspected alt: " + e.getMessage());
        }
    }
}
