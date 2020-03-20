package org.infinispan.tutorial.client;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.commons.api.CacheContainerAdmin;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ClientWeatherApp {

    private static final String[] locations = {"Rome, Italy", "Como, Italy", "Basel, Switzerland", "Bern, Switzerland",
            "London, UK", "UK", "Bucarest, Romania", "Cluj-Napoca, Romania", "Ottawa, Canada",
            "Toronto, Canada", "Lisbon, Portugal", "Porto, Portugal", "Raleigh, USA", "Washington, USA"};

    private final WeatherService weatherService;
    private final RemoteCacheManager remoteCacheManager;
    private final RemoteCache<String, LocationWeather> cache;

    public ClientWeatherApp() throws InterruptedException, IOException {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer().host("127.0.0.1")
                .port(ConfigurationProperties.DEFAULT_HOTROD_PORT);


        // Connect to the server
        remoteCacheManager = new RemoteCacheManager(builder.build());
        // Get the cache, create it if needed with an existing template name
        cache = remoteCacheManager.administration()
                .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
                .getOrCreateCache("weather", "org.infinispan.DIST_SYNC");


        weatherService = initWeatherService(cache);
    }

    private WeatherService initWeatherService(RemoteCache<String, LocationWeather> cache) {
        String apiKey = System.getenv("OWMAPIKEY");
        if (apiKey == null) {
            System.out.println("WARNING: OWMAPIKEY environment variable not set, using the RandomWeatherService.");
            return new RandomWeatherService(cache);
        } else {
            return new OpenWeatherMapService(apiKey, cache);
        }
    }

    public void fetchWeather() {
        System.out.println("---- Fetching weather information ----");
        long start = System.currentTimeMillis();
        for (String location : locations) {
            LocationWeather weather = weatherService.getWeatherForLocation(location);
            System.out.printf("%s - %s\n", location, weather);
        }
        System.out.printf("---- Fetched in %dms ----\n", System.currentTimeMillis() - start);
    }

    public void computeCountryAverages() {

    }

    public void shutdown() {
        remoteCacheManager.stop();
    }

    public static void main(String[] args) throws Exception {
        ClientWeatherApp app = new ClientWeatherApp();
        try {
            app.fetchWeather();

            app.fetchWeather();

            TimeUnit.SECONDS.sleep(5);

            app.fetchWeather();

            app.computeCountryAverages();

        } finally {
            app.shutdown();
        }

    }
}