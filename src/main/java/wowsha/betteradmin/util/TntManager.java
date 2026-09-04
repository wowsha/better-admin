package wowsha.betteradmin.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.TntEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TntManager {
    private static final int INTERVAL_TICKS = 80;
    private static final int MAX_ACTIVE_PLAYERS = 16;
    private static final int MAX_ACTIVE_TNT = 64;
    private static final Map<UUID, Integer> timers = new HashMap<>();

    private TntManager() {}

    public static void toggle(ServerPlayer player) {
        if (timers.containsKey(player.getUUID())) {
            timers.remove(player.getUUID());
        } else if (timers.size() < MAX_ACTIVE_PLAYERS) {
            timers.put(player.getUUID(), 0);
        }
    }

    public static void stop(ServerPlayer player) {
        timers.remove(player.getUUID());
    }

    public static void stopAll() {
        timers.clear();
    }

    public static boolean isActive(ServerPlayer player) {
        return timers.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || timers.isEmpty()) return;

        int activeTnt = 0;
        for (net.minecraft.server.MinecraftServer server : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() == null
                ? java.util.List.<net.minecraft.server.MinecraftServer>of()
                : java.util.List.of(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer())) {
            for (net.minecraft.world.level.Level level : server.getAllLevels()) {
                activeTnt += (int) level.getEntitiesOfClass(TntEntity.class, level.getWorldBorder().getCollisionShape().bounds()).size();
            }
        }

        for (UUID uuid : java.util.List.copyOf(timers.keySet())) {
            ServerPlayer player = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() == null
                    ? null
                    : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
            if (player == null) {
                timers.remove(uuid);
                continue;
            }

            int ticks = timers.getOrDefault(uuid, 0) + 1;
            if (ticks >= INTERVAL_TICKS) {
                if (activeTnt < MAX_ACTIVE_TNT) {
                    TntEntity tnt = new TntEntity(player.level(), player.getX(), player.getY(), player.getZ());
                    player.level().addFreshEntity(tnt);
                    activeTnt++;
                }
                ticks = 0;
            }
            timers.put(uuid, ticks);
        }
    }
}
