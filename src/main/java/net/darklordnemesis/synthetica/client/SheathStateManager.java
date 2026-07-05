package net.darklordnemesis.synthetica.client;

import net.darklordnemesis.synthetica.network.KatanaSyncInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SheathStateManager {

    // Now stores a list of katana states per player UUID
    private static final Map<UUID, List<KatanaSyncInfo>> KATANA_CACHE = new ConcurrentHashMap<>();

    public static void setKatanas(UUID playerId, List<KatanaSyncInfo> katanas) {
        KATANA_CACHE.put(playerId, katanas);
    }

    public static List<KatanaSyncInfo> getKatanas(UUID playerId) {
        return KATANA_CACHE.getOrDefault(playerId, Collections.emptyList());
    }

    public static void removePlayer(UUID playerId) {
        KATANA_CACHE.remove(playerId);
    }
}