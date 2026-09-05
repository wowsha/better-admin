package wowsha.betteradmin.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class HackerManager {
    private static final int MAX_HACKERS = 5;
    private static final int DROP_INTERVAL = 4;
    private static final int MAX_ITEM_ENTITIES = 256;
    private static final double FLY_SPEED = 0.55D;
    private static final Random RANDOM = new Random();
    private static final List<Hacker> HACKERS = new ArrayList<>();
    private static int dropTimer;

    private HackerManager() {}

    public static void start(MinecraftServer server) {
        persist(server, true);
        spawn(server);
    }

    public static void stop(MinecraftServer server) {
        persist(server, false);
        removeAll(server);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (readPersisted(server)) spawn(server);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || HACKERS.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        List<ChunkRef> active = activeChunks(server);
        if (active.isEmpty()) return;

        for (Hacker hacker : List.copyOf(HACKERS)) {
            FakePlayer player = hacker.player;
            if (player.isRemoved()) {
                HACKERS.remove(hacker);
                continue;
            }

            ChunkRef current = findContaining(active, player.serverLevel(), player.blockPosition());
            if (current == null) {
                ChunkRef nearest = closest(active, player.serverLevel(), player.getX(), player.getZ());
                if (nearest == null) nearest = active.get(0);
                teleportToChunk(player, nearest);
            } else {
                flyAround(player, active);
            }

            player.setNoGravity(true);
            Abilities abilities = player.getAbilities();
            abilities.mayfly = true;
            abilities.flying = true;
        }

        dropTimer++;
        if (dropTimer >= DROP_INTERVAL) {
            dropTimer = 0;
            for (int i = 0; i < 2; i++) dropLoot(active, server);
        }
    }

    private static void spawn(MinecraftServer server) {
        if (!HACKERS.isEmpty()) return;
        List<ChunkRef> active = activeChunks(server);
        if (active.isEmpty()) return;

        for (int i = 0; i < MAX_HACKERS; i++) {
            ChunkRef chunk = active.get(i % active.size());
            double x = chunk.pos.getMiddleBlockX() + RANDOM.nextDouble() * 8.0D - 4.0D;
            double z = chunk.pos.getMiddleBlockZ() + RANDOM.nextDouble() * 8.0D - 4.0D;
            int height = chunk.level.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    new net.minecraft.core.BlockPos((int) x, 0, (int) z)).getY();
            double y = height + 8.0D + RANDOM.nextDouble() * 12.0D;

            FakePlayer hacker = new FakePlayer(
                    chunk.level,
                    new GameProfile(UUID.randomUUID(), "Hacker_" + String.format("%02d", i + 1)));
            hacker.setPos(x, y, z);
            hacker.setNoGravity(true);
            hacker.setSilent(true);
            hacker.setInvulnerable(true);
            Abilities abilities = hacker.getAbilities();
            abilities.mayfly = true;
            abilities.flying = true;
            abilities.setFlyingSpeed(0.35F);
            chunk.level.addFreshEntity(hacker);
            HACKERS.add(new Hacker(hacker));
        }

        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            for (Hacker hacker : HACKERS) {
                viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(
                        Collections.singletonList(hacker.player)));
            }
        }

        for (Hacker hacker : HACKERS) {
            broadcast(server, hacker.player.getGameProfile().getName() + " joined the game");
        }
    }

    private static void removeAll(MinecraftServer server) {
        for (Hacker hacker : List.copyOf(HACKERS)) {
            UUID uuid = hacker.player.getUUID();
            String name = hacker.player.getGameProfile().getName();
            if (!hacker.player.isRemoved()) hacker.player.discard();
            for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
                viewer.connection.send(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(uuid)));
            }
            broadcast(server, name + " left the game");
        }
        HACKERS.clear();
        dropTimer = 0;
    }

    private static void broadcast(MinecraftServer server, String text) {
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(text).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
    }

    private static void flyAround(FakePlayer player, List<ChunkRef> active) {
        ChunkRef target = closest(active, player.serverLevel(), player.getX(), player.getZ());
        if (target == null) return;

        long seed = player.getUUID().getLeastSignificantBits();
        float phase = player.tickCount * 0.08F + (seed & 0xFFFF) * 0.001F;
        double targetX = target.pos.getMiddleBlockX() + Mth.sin(phase) * 18.0D;
        double targetZ = target.pos.getMiddleBlockZ() + Mth.cos(phase * 0.87F) * 18.0D;
        double targetY = player.getY() + Mth.sin(phase * 1.7F) * 0.7D;

        double dx = targetX - player.getX();
        double dy = targetY - player.getY();
        double dz = targetZ - player.getZ();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 0.001D) return;

        player.setDeltaMovement(dx / length * FLY_SPEED, dy / length * FLY_SPEED, dz / length * FLY_SPEED);
        player.hurtMarked = true;
        player.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        player.setXRot((float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz))));
    }

    private static void dropLoot(List<ChunkRef> active, MinecraftServer server) {
        if (countItemEntities(server) >= MAX_ITEM_ENTITIES) return;
        ChunkRef chunk = active.get(RANDOM.nextInt(active.size()));
        int x = chunk.pos.getMinBlockX() + RANDOM.nextInt(16);
        int z = chunk.pos.getMinBlockZ() + RANDOM.nextInt(16);
        int y = chunk.level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new net.minecraft.core.BlockPos(x, 0, z)).getY() + 1;
        ItemEntity item = new ItemEntity(chunk.level, x + 0.5D, y + 0.2D, z + 0.5D, randomLoot());
        item.setPickUpDelay(10);
        item.setDeltaMovement((RANDOM.nextDouble() - 0.5D) * 0.25D, 0.22D + RANDOM.nextDouble() * 0.18D,
                (RANDOM.nextDouble() - 0.5D) * 0.25D);
        chunk.level.addFreshEntity(item);
    }

    private static ItemStack randomLoot() {
        return switch (RANDOM.nextInt(10)) {
            case 0 -> new ItemStack(Items.DIAMOND, 4 + RANDOM.nextInt(9));
            case 1 -> new ItemStack(Items.NETHERITE_INGOT, 1 + RANDOM.nextInt(4));
            case 2 -> new ItemStack(Items.NETHERITE_BLOCK, 1 + RANDOM.nextInt(3));
            case 3 -> new ItemStack(Items.DIAMOND_BLOCK, 2 + RANDOM.nextInt(5));
            case 4 -> new ItemStack(Items.BEACON);
            case 5 -> new ItemStack(Items.BARRIER, 1 + RANDOM.nextInt(4));
            case 6 -> new ItemStack(Items.COMMAND_BLOCK);
            case 7 -> new ItemStack(Items.CHAIN_COMMAND_BLOCK);
            case 8 -> new ItemStack(Items.REPEATING_COMMAND_BLOCK);
            default -> new ItemStack(Items.NETHERITE_BLOCK);
        };
    }

    private static int countItemEntities(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity) count++;
            }
        }
        return count;
    }

    private static List<ChunkRef> activeChunks(MinecraftServer server) {
        List<ChunkRef> result = new ArrayList<>();
        for (var chunk : ActiveChunkUtil.collect(server)) {
            result.add(new ChunkRef(chunk.getLevel(), chunk.getPos()));
        }
        return result;
    }

    private static ChunkRef findContaining(List<ChunkRef> active, ServerLevel level, net.minecraft.core.BlockPos pos) {
        int x = pos.getX() >> 4;
        int z = pos.getZ() >> 4;
        for (ChunkRef chunk : active) {
            if (chunk.level == level && chunk.pos.x == x && chunk.pos.z == z) return chunk;
        }
        return null;
    }

    private static ChunkRef closest(List<ChunkRef> active, ServerLevel level, double x, double z) {
        ChunkRef best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ChunkRef chunk : active) {
            if (chunk.level != level) continue;
            double dx = x - chunk.pos.getMiddleBlockX();
            double dz = z - chunk.pos.getMiddleBlockZ();
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = chunk;
            }
        }
        return best;
    }

    private static void teleportToChunk(FakePlayer player, ChunkRef chunk) {
        double x = chunk.pos.getMiddleBlockX() + RANDOM.nextDouble() * 8.0D - 4.0D;
        double z = chunk.pos.getMiddleBlockZ() + RANDOM.nextDouble() * 8.0D - 4.0D;
        int y = chunk.level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                new net.minecraft.core.BlockPos((int) x, 0, (int) z)).getY() + 8;
        player.teleportTo(chunk.level, x, y, z, player.getYRot(), player.getXRot());
    }

    private static Path stateFile(MinecraftServer server) {
        return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("troll_commands_hackers.active");
    }

    private static void persist(MinecraftServer server, boolean active) {
        try {
            if (active) Files.writeString(stateFile(server), "active\n");
            else Files.deleteIfExists(stateFile(server));
        } catch (IOException ignored) {
        }
    }

    private static boolean readPersisted(MinecraftServer server) {
        try {
            return Files.exists(stateFile(server)) && Files.readString(stateFile(server)).trim().equals("active");
        } catch (IOException ignored) {
            return false;
        }
    }

    private record Hacker(FakePlayer player) {}
    private record ChunkRef(ServerLevel level, ChunkPos pos) {}
}
