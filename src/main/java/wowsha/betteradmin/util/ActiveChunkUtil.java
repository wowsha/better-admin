package wowsha.betteradmin.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ActiveChunkUtil {
    private static final int MAX_CHUNKS = 64;

    private ActiveChunkUtil() {}

    public static List<LevelChunk> collect(MinecraftServer server) {
        Map<String, LevelChunk> chunks = new LinkedHashMap<>();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int radius = Math.max(2, Math.min(server.getPlayerList().getSimulationDistance(), 8));

        for (ServerPlayer player : players) {
            ServerLevel level = player.serverLevel();
            ChunkPos center = new ChunkPos(player.blockPosition());
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int chunkX = center.x + dx;
                    int chunkZ = center.z + dz;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk != null) {
                        String key = level.dimension().location() + ":" + chunkX + ":" + chunkZ;
                        chunks.putIfAbsent(key, chunk);
                    }
                }
            }
        }

        List<LevelChunk> result = new ArrayList<>(chunks.values());
        if (result.size() <= MAX_CHUNKS) return result;

        result.sort(Comparator.comparingDouble(chunk -> nearestPlayerDistanceSq(chunk, players)));
        return new ArrayList<>(result.subList(0, MAX_CHUNKS));
    }

    private static double nearestPlayerDistanceSq(LevelChunk chunk, List<ServerPlayer> players) {
        double best = Double.MAX_VALUE;
        double centerX = chunk.getPos().getMiddleBlockX();
        double centerZ = chunk.getPos().getMiddleBlockZ();
        for (ServerPlayer player : players) {
            if (player.serverLevel() != chunk.getLevel()) continue;
            double dx = player.getX() - centerX;
            double dz = player.getZ() - centerZ;
            best = Math.min(best, dx * dx + dz * dz);
        }
        return best;
    }
}
