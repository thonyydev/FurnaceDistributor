package com.thonyy.furnacedistributor.config;

import dev.architectury.platform.Platform;

public final class ServerConfig {
    private static ServerConfig instance = new ServerConfig();
    public int maxDistance = 32;
    public int requestCooldownTicks = 5;
    public boolean smartDistribution = true;
    public boolean allowPartialDistribution = false;
    public boolean collectExperience = true;

    public static void load() {
        instance = new ConfigFile<>(Platform.getConfigFolder().resolve("furnacedistributor-server.json"),
                ServerConfig.class, ServerConfig::validate).load(ServerConfig::new);
    }

    public void validate() {
        maxDistance = Math.clamp(maxDistance, 8, 128);
        requestCooldownTicks = Math.clamp(requestCooldownTicks, 0, 100);
    }

    public static ServerConfig get() { return instance; }
}
