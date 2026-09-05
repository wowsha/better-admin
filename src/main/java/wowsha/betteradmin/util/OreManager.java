package wowsha.betteradmin.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

public final class OreManager {
    private static final List<Snapshot> ORIGINAL_ORES = new ArrayList<>();

    private OreManager() {}

    public static void removeActiveOres(MinecraftServer server) {
        ORIGINAL_ORES.clear();
        for (LevelChunk chunk : ActiveChunkUtil.collect(server)) {
            ServerLevel level = chunk.getLevel();
            int minX = chunk.getPos().getMinBlockX();
            int minZ = chunk.getPos().getMinBlockZ();
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (!state.is(BlockTags.ORES)) continue;
                        ORIGINAL_ORES.add(new Snapshot(level, pos.immutable(), state));
                        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
    }

    public static void regenerate() {
        for (Snapshot snapshot : List.copyOf(ORIGINAL_ORES)) {
            if (!snapshot.level.hasChunkAt(snapshot.pos)) continue;
            snapshot.level.setBlockAndUpdate(snapshot.pos, snapshot.state);
        }
        ORIGINAL_ORES.clear();
    }

    public static void clear() {
        ORIGINAL_ORES.clear();
    }

    private record Snapshot(ServerLevel level, BlockPos pos, BlockState state) {}
}
