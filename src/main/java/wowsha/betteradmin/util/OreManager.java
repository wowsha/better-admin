package wowsha.betteradmin.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OreManager {
    private static final Map<String, BlockState> ORIGINAL_ORES = new HashMap<>();

    private OreManager() {}

    public static void removeActiveOres(MinecraftServer server) {
        for (LevelChunk chunk : ActiveChunkUtil.collect(server)) {
            var level = chunk.getLevel();
            int minX = chunk.getPos().getMinBlockX();
            int minZ = chunk.getPos().getMinBlockZ();
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (!state.is(BlockTags.ORES)) continue;
                        ORIGINAL_ORES.putIfAbsent(key(level, pos), state);
                        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
    }

    public static void regenerate() {
        for (Map.Entry<String, BlockState> entry : List.copyOf(ORIGINAL_ORES.entrySet())) {
            SnapshotPos snapshot = parse(entry.getKey());
            if (snapshot == null || !snapshot.level.hasChunkAt(snapshot.pos)) continue;
            if (snapshot.level.getBlockState(snapshot.pos).is(BlockTags.ORES)) continue;
            snapshot.level.setBlockAndUpdate(snapshot.pos, entry.getValue());
        }
        ORIGINAL_ORES.clear();
    }

    public static List<BlockPos> currentSavedOrePositions() {
        List<BlockPos> result = new ArrayList<>();
        for (String key : ORIGINAL_ORES.keySet()) {
            SnapshotPos parsed = parse(key);
            if (parsed != null) result.add(parsed.pos);
        }
        return result;
    }

    public static void clear() {
        ORIGINAL_ORES.clear();
    }

    private static String key(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    private static SnapshotPos parse(String value) {
        String[] parts = value.split("\\|", 4);
        if (parts.length != 4) return null;
        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(parts[1]);
            y = Integer.parseInt(parts[2]);
            z = Integer.parseInt(parts[3]);
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private record SnapshotPos(net.minecraft.server.level.ServerLevel level, BlockPos pos) {}
}
