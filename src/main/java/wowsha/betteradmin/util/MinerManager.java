package wowsha.betteradmin.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MinerManager {
    private static final int MAX_MINERS = 5;
    private static final int TARGET_RESCAN_TICKS = 40;
    private static final double MOVE_SPEED = 0.18D;
    private static final List<Miner> MINERS = new ArrayList<>();
    private static final Map<UUID, BlockPos> TARGETS = new HashMap<>();
    private static final Map<UUID, Integer> TARGET_TIMERS = new HashMap<>();

    private MinerManager() {}

    public static void spawn(ServerPlayer owner) {
        stopAll();
        int spawned = 0;
        for (int i = 1; i <= MAX_MINERS; i++) {
            FakePlayer miner = new FakePlayer(owner.serverLevel(),
                    new GameProfile(UUID.randomUUID(), "Miner_" + i));
            miner.setPos(owner.getX() + (i - 3) * 1.5D, owner.getY(), owner.getZ() + 3.0D);
            miner.setSilent(true);
            miner.setInvulnerable(true);
            owner.serverLevel().addFreshEntity(miner);
            MINERS.add(new Miner(miner, owner.serverLevel()));
            spawned++;
        }
    }

    public static void stopAll() {
        for (Miner miner : List.copyOf(MINERS)) {
            if (!miner.player.isRemoved()) miner.player.discard();
        }
        MINERS.clear();
        TARGETS.clear();
        TARGET_TIMERS.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || MINERS.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        List<Miner> snapshot = List.copyOf(MINERS);
        for (Miner miner : snapshot) {
            FakePlayer player = miner.player;
            if (player.isRemoved()) {
                remove(miner);
                continue;
            }

            boolean seen = isSeenByPlayer(player, server);
            player.setInvisible(seen);
            if (seen) continue;

            int timer = TARGET_TIMERS.getOrDefault(player.getUUID(), 0) + 1;
            if (timer >= TARGET_RESCAN_TICKS || !isValidTarget(player, TARGETS.get(player.getUUID()))) {
                TARGETS.put(player.getUUID(), findNearestOre(player));
                timer = 0;
            }
            TARGET_TIMERS.put(player.getUUID(), timer);

            BlockPos target = TARGETS.get(player.getUUID());
            if (target == null) continue;
            if (!isValidTarget(player, target)) {
                TARGETS.remove(player.getUUID());
                continue;
            }

            double dx = target.getX() + 0.5D - player.getX();
            double dy = target.getY() + 0.5D - player.getY();
            double dz = target.getZ() + 0.5D - player.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);

            if (horizontal < 2.0D && Math.abs(dy) < 2.5D) {
                mine(player, target);
                TARGETS.remove(player.getUUID());
                continue;
            }

            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length <= 0.001D) continue;
            double vx = dx / length * MOVE_SPEED;
            double vz = dz / length * MOVE_SPEED;
            double vy = Math.max(-0.08D, Math.min(0.08D, dy / length * MOVE_SPEED));
            player.setYRot((float) (Math.toDegrees(Math.atan2(-vx, vz))));
            player.move(MoverType.SELF, new net.minecraft.world.phys.Vec3(vx, vy, vz));
        }

        if (MINERS.isEmpty()) return;
        boolean anyOre = false;
        for (Miner miner : MINERS) {
            if (findNearestOre(miner.player) != null) {
                anyOre = true;
                break;
            }
        }
        if (!anyOre) stopAll();
    }

    private static void mine(FakePlayer miner, BlockPos pos) {
        BlockState state = miner.serverLevel().getBlockState(pos);
        if (!state.is(BlockTags.ORES)) return;
        miner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        miner.serverLevel().destroyBlock(pos, true, miner);
    }

    private static BlockPos findNearestOre(FakePlayer miner) {
        ServerLevel level = miner.serverLevel();
        BlockPos center = miner.blockPosition();
        int chunkRadius = 2;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int chunkX = (center.getX() >> 4) - chunkRadius; chunkX <= (center.getX() >> 4) + chunkRadius; chunkX++) {
            for (int chunkZ = (center.getZ() >> 4) - chunkRadius; chunkZ <= (center.getZ() >> 4) + chunkRadius; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) continue;
                int minX = chunkX << 4;
                int minZ = chunkZ << 4;
                for (int x = minX; x < minX + 16; x++) {
                    for (int z = minZ; z < minZ + 16; z++) {
                        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (!level.getBlockState(pos).is(BlockTags.ORES)) continue;
                            double dist = pos.distSqr(center);
                            if (dist < bestDistance) {
                                bestDistance = dist;
                                best = pos.immutable();
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private static boolean isValidTarget(FakePlayer miner, BlockPos target) {
        return target != null && miner.serverLevel().hasChunkAt(target) && miner.serverLevel().getBlockState(target).is(BlockTags.ORES);
    }

    private static boolean isSeenByPlayer(Entity miner, MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.serverLevel() != miner.level()) continue;
            if (player.distanceToSqr(miner) > 32.0D * 32.0D) continue;
            if (player.hasLineOfSight(miner)) return true;
        }
        return false;
    }

    private static void remove(Miner miner) {
        MINERS.remove(miner);
        TARGETS.remove(miner.player.getUUID());
        TARGET_TIMERS.remove(miner.player.getUUID());
    }

    private record Miner(FakePlayer player, ServerLevel level) {}
}
