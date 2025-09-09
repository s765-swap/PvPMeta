package com.swapnil.titlemod.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class PremiumChecker {
    private static final String PROFILE_ENDPOINT_PREFIX = "https://api.mojang.com/users/profiles/minecraft/";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final long CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    private static final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private PremiumChecker() {}

    private static class CacheEntry {
        final boolean premium;
        final long cachedAt;
        CacheEntry(boolean premium) {
            this.premium = premium;
            this.cachedAt = System.currentTimeMillis();
        }
        boolean isFresh() { return System.currentTimeMillis() - cachedAt < CACHE_TTL_MS; }
    }

    public static boolean isPremium(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        try {
            CacheEntry cached = cache.get(username.toLowerCase());
            if (cached != null && cached.isFresh()) {
                return cached.premium;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROFILE_ENDPOINT_PREFIX + username))
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean premium = (response.statusCode() == 200) && response.body() != null && !response.body().isBlank();
            cache.put(username.toLowerCase(), new CacheEntry(premium));
            return premium;
        } catch (Exception e) {
            
            SecureLogger.logSecurityEvent("Premium check failed: " + e.getMessage());
            return false;
        }
    }
}


