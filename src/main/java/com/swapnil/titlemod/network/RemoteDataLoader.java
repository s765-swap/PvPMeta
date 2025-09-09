package com.swapnil.titlemod.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import com.swapnil.titlemod.security.IPMasker;
import com.swapnil.titlemod.security.HTTPRequestProtector;
import com.swapnil.titlemod.data.DuelEntry; 
import com.swapnil.titlemod.data.MatchLogEntryDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoteDataLoader {
    private static final Logger LOGGER = LogManager.getLogger("TitleMod RemoteDataLoader");
    private static final String BASE_URL = "http://34.159.92.94:4560";

    private static final Gson GSON = new Gson();
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private static final int MAX_RETRIES = 2;
    private static final long INITIAL_BACKOFF_DELAY_MS = 200;
    
    
    private static Map<String, Integer> cachedEloData = null;
    private static long lastEloFetchTime = 0;
    private static final long CACHE_DURATION_MS = 30000; 

    
    private static final Map<String, Integer> DUMMY_ELO_DATA;
    private static final List<MatchLogEntryDTO> DUMMY_MATCH_LOG_DATA;
    
    private static final Map<String, String> DUMMY_USERNAME_DATA;

    static {
        
        String penmdUuid = "a98b7147da2d4479803106c468908762";
        String cxlmYourselfUuid = "e179086ee0c238d1bbf4740675af4db6"; 
        String penmdUsername = "penmd";
        String cxlmYourselfUsername = "CxlmYourself";

        DUMMY_ELO_DATA = new HashMap<>();
        DUMMY_ELO_DATA.put(penmdUuid, 985); 
        DUMMY_ELO_DATA.put(cxlmYourselfUuid, 1015); 
        DUMMY_ELO_DATA.put("f84c6a79b2e4c1d0a9b8c7d6e5f4a3b2", 1200); 
        
        DUMMY_ELO_DATA.put("1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d", 1800); 

        DUMMY_MATCH_LOG_DATA = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
       
        DUMMY_MATCH_LOG_DATA.add(new MatchLogEntryDTO(cxlmYourselfUsername, penmdUsername, cxlmYourselfUsername, penmdUsername, currentTime));
        DUMMY_MATCH_LOG_DATA.add(new MatchLogEntryDTO(penmdUsername, "NyxTara", penmdUsername, "NyxTara", currentTime - 60000)); 

        
        DUMMY_USERNAME_DATA = new HashMap<>();
        DUMMY_USERNAME_DATA.put(penmdUuid, penmdUsername);
        DUMMY_USERNAME_DATA.put(cxlmYourselfUuid, cxlmYourselfUsername);
        DUMMY_USERNAME_DATA.put("f84c6a79b2e4c1d0a9b8c7d6e5f4a3b2", "Player1");
        DUMMY_USERNAME_DATA.put("1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d", "Player2");
    }


    /**
     * @param callback A consumer to handle the fetched ELO data (Map of UUID string to ELO).
     */
    public static void fetchEloData(Consumer<Map<String, Integer>> callback) {
       
        long currentTime = System.currentTimeMillis();
        if (cachedEloData != null && (currentTime - lastEloFetchTime) < CACHE_DURATION_MS) {
            LOGGER.info("Using cached ELO data (age: " + (currentTime - lastEloFetchTime) + "ms)");
            MinecraftClient.getInstance().execute(() -> callback.accept(cachedEloData));
            return;
        }
        
        CompletableFuture.<Map<String, Integer>>supplyAsync(() -> {
            String playerUuid = MinecraftClient.getInstance().getSession().getUuid(); 
            String urlString = BASE_URL + "/elo.json";
            if (playerUuid != null && !playerUuid.isEmpty()) {
                urlString += "?uuid=" + playerUuid; 
            }
            LOGGER.info("Attempting to fetch ELO data from backend: " + IPMasker.maskURL(urlString));

            long currentDelay = INITIAL_BACKOFF_DELAY_MS;
            for (int i = 0; i < MAX_RETRIES; i++) {
                try {
                    
                    HttpURLConnection conn = HTTPRequestProtector.createSecureConnection(urlString);
                    conn.setRequestMethod("GET");
                    
                   

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
                            Map<String, Integer> eloMap = GSON.fromJson(reader, type);
                            LOGGER.info("Successfully fetched ELO data from " + IPMasker.maskURL(urlString));
                            
                            cachedEloData = eloMap;
                            lastEloFetchTime = System.currentTimeMillis();
                            return eloMap;
                        }
                    } else {
                        LOGGER.warn("Attempt " + (i + 1) + " failed for ELO data. HTTP Response Code: " + responseCode + ". Retrying in " + currentDelay + "ms...");
                        Thread.sleep(currentDelay);
                        currentDelay *= 2;
                    }
                } catch (IOException | InterruptedException e) {
                    LOGGER.warn("Attempt " + (i + 1) + " failed for ELO data: " + e.getMessage() + ". Retrying in " + currentDelay + "ms...");
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    try {
                        Thread.sleep(currentDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Collections.emptyMap();
                    }
                    currentDelay *= 2;
                }
            }
            LOGGER.error("Failed to fetch ELO data from backend after " + MAX_RETRIES + " retries. Falling back to dummy data.");
            return DUMMY_ELO_DATA;
        }, EXECUTOR).thenAccept(result -> MinecraftClient.getInstance().execute(() -> callback.accept(result)));
    }

    /**
     * @param callback A consumer to handle the fetched list of MatchLogEntryDTO objects.
     */
    public static void fetchRealTimeDuels(Consumer<List<MatchLogEntryDTO>> callback) {
        CompletableFuture.<List<MatchLogEntryDTO>>supplyAsync(() -> {
            LOGGER.info("Attempting to fetch duel data from backend: " + IPMasker.maskURL(BASE_URL + "/real_time_duels.json"));
            long currentDelay = INITIAL_BACKOFF_DELAY_MS;
            for (int i = 0; i < MAX_RETRIES; i++) {
                try {
                    
                    HttpURLConnection conn = HTTPRequestProtector.createSecureConnection(BASE_URL + "/real_time_duels.json");
                    conn.setRequestMethod("GET");
                    
                   
                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                            Type type = new TypeToken<List<MatchLogEntryDTO>>() {}.getType();
                            List<MatchLogEntryDTO> duelEntries = GSON.fromJson(reader, type);
                            LOGGER.info("Successfully fetched duel data from " + IPMasker.maskURL(BASE_URL + "/real_time_duels.json"));
                            return duelEntries;
                        }
                    } else {
                        LOGGER.warn("Attempt " + (i + 1) + " failed for duel data. HTTP Response Code: " + responseCode + ". Retrying in " + currentDelay + "ms...");
                        Thread.sleep(currentDelay);
                        currentDelay *= 2;
                    }
                } catch (IOException | InterruptedException e) {
                    LOGGER.warn("Attempt " + (i + 1) + " failed for duel data: " + e.getMessage() + ". Retrying in " + currentDelay + "ms...");
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    try {
                        Thread.sleep(currentDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Collections.emptyList();
                    }
                    currentDelay *= 2;
                }
            }
            LOGGER.error("Failed to fetch duel data from backend after " + MAX_RETRIES + " retries.");
            return Collections.emptyList();
        }, EXECUTOR).thenAccept(result -> MinecraftClient.getInstance().execute(() -> callback.accept(result)));
    }

    /**
     * @param callback A consumer to handle the fetched username data (Map of UUID string to Username).
     */
    public static void fetchUsernames(Consumer<Map<String, String>> callback) {
        CompletableFuture.<Map<String, String>>supplyAsync(() -> {
            String urlString = BASE_URL + "/usernames.json";
            LOGGER.info("Attempting to fetch usernames from backend: " + IPMasker.maskURL(urlString));

            long currentDelay = INITIAL_BACKOFF_DELAY_MS;
            for (int i = 0; i < MAX_RETRIES; i++) {
                try {
                   
                    HttpURLConnection conn = HTTPRequestProtector.createSecureConnection(urlString);
                    conn.setRequestMethod("GET");

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                            Type type = new TypeToken<Map<String, String>>() {}.getType();
                            Map<String, String> usernameMap = GSON.fromJson(reader, type);
                            LOGGER.info("Successfully fetched usernames from " + IPMasker.maskURL(urlString));
                            return usernameMap;
                        }
                    } else {
                        LOGGER.warn("Attempt " + (i + 1) + " failed for usernames. HTTP Response Code: " + responseCode + ". Retrying in " + currentDelay + "ms...");
                        Thread.sleep(currentDelay);
                        currentDelay *= 2;
                    }
                } catch (IOException | InterruptedException e) {
                    LOGGER.warn("Attempt " + (i + 1) + " failed for usernames: " + e.getMessage() + ". Retrying in " + currentDelay + "ms...");
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    try {
                        Thread.sleep(currentDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Collections.emptyMap();
                    }
                    currentDelay *= 2;
                }
            }
            LOGGER.error("Failed to fetch usernames from backend after " + MAX_RETRIES + " retries. Falling back to dummy data.");
            return DUMMY_USERNAME_DATA;
        }, EXECUTOR).thenAccept(result -> MinecraftClient.getInstance().execute(() -> callback.accept(result)));
    }

    
    public static void clearEloCache() {
        cachedEloData = null;
        lastEloFetchTime = 0;
        LOGGER.info("ELO cache cleared.");
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("RemoteDataLoader executor shut down.");
    }
}
