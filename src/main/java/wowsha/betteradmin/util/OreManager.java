package wowsha.betteradmin.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

public final class OreManager {
    private static final List<Snapshot> ORIGINAL_ORES = new ArrayList<>();

    private OreManager() {}

    public static void removeActiveOres(MinecraftServer server) {
        for (LevelChunk chunk : ActiveChunkUtil.collect(server)) {
            Level rawLevel = chunk.getLevel();
            if (!(rawLevel instanceof ServerLevel level)) continue;
            int minX = chunk.getPos().getMinBlockX();
            int minZ = chunk.getPos().getMinBlockZ();
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (!isOre(state)) continue;
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
            BlockState current = snapshot.level.getBlockState(snapshot.pos);
            if (current.is(Blocks.STONE)) {
                snapshot.level.setBlockAndUpdate(snapshot.pos, snapshot.state);
            }
        }
        ORIGINAL_ORES.clear();
    }

    public static void clear() {
        ORIGINAL_ORES.clear();
    }

    public static boolean isOre(BlockState state) {
        return isOreBlock(state.getBlock());
    }

    private static boolean isOreBlock(Block block) {
        return block == Blocks.COAL_ORE
                || block == Blocks.DEEPSLATE_COAL_ORE
                || block == Blocks.IRON_ORE
                || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.GOLD_ORE
                || block == Blocks.DEEPSLATE_GOLD_ORE
                || block == Blocks.DIAMOND_ORE
                || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.EMERALD_ORE
                || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.LAPIS_ORE
                || block == Blocks.DEEPSLATE_LAPIS_ORE
                || block == Blocks.REDSTONE_ORE
                || block == Blocks.DEEPSLATE_REDSTONE_ORE
                || block == Blocks.COPPER_ORE
                || block == Blocks.DEEPSLATE_COPPER_ORE
                || block == Blocks.NETHER_GOLD_ORE
                || block == Blocks.NETHER_QUARTZ_ORE
                || block == Blocks.ANCIENT_DEBRIS;
    }

    private record Snapshot(ServerLevel level, BlockPos pos, BlockState state) {}
}
