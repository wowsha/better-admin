package wowsha.betteradmin.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ItemRainManager {
    private static final int DROP_INTERVAL_TICKS = 20;
    private static final int ITEMS_PER_CHUNK = 6;
    private static final int MAX_ITEM_ENTITIES = 1024;
    private static final Random RANDOM = new Random();
    private static boolean active;
    private static int timer;

    private ItemRainManager() {}

    public static void start() {
        active = true;
        timer = 0;
    }

    public static void stop() {
        active = false;
        timer = 0;
    }

    public static boolean isActive() {
        return active;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        timer++;
        if (timer < DROP_INTERVAL_TICKS) return;
        timer = 0;

        int totalItems = countItemEntities(server);
        if (totalItems >= MAX_ITEM_ENTITIES) return;

        for (var chunk : ActiveChunkUtil.collect(server)) {
            ServerLevel level = chunk.getLevel() instanceof ServerLevel serverLevel ? serverLevel : null;
            if (level == null) continue;

            int minX = chunk.getPos().getMinBlockX();
            int minZ = chunk.getPos().getMinBlockZ();

            for (int i = 0; i < ITEMS_PER_CHUNK && totalItems < MAX_ITEM_ENTITIES; i++) {
                int x = minX + RANDOM.nextInt(16);
                int z = minZ + RANDOM.nextInt(16);
                int groundY = level.getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new net.minecraft.core.BlockPos(x, 0, z)).getY();
                double y = groundY + 12.0D + RANDOM.nextDouble() * 6.0D;

                ItemEntity item = new ItemEntity(
                        level,
                        x + 0.5D,
                        y,
                        z + 0.5D,
                        randomItem());
                item.setPickUpDelay(5);
                item.setDeltaMovement(
                        (RANDOM.nextDouble() - 0.5D) * 0.15D,
                        -0.05D - RANDOM.nextDouble() * 0.08D,
                        (RANDOM.nextDouble() - 0.5D) * 0.15D);
                level.addFreshEntity(item);
                totalItems++;
            }
        }
    }

    private static ItemStack randomItem() {
        List<Item> items = new ArrayList<>();
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item != Items.AIR) items.add(item);
        }
        if (items.isEmpty()) return Items.STONE.getDefaultInstance();
        return new ItemStack(items.get(RANDOM.nextInt(items.size())), 1 + RANDOM.nextInt(3));
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
}
