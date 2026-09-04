package wowsha.betteradmin.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.List;
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
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        int activeTnt = 0;
        for (var level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof PrimedTnt) activeTnt++;
            }
        }

        for (UUID uuid : List.copyOf(timers.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                timers.remove(uuid);
                continue;
            }

            int ticks = timers.getOrDefault(uuid, 0) + 1;
            if (ticks >= INTERVAL_TICKS) {
                if (activeTnt < MAX_ACTIVE_TNT) {
                    PrimedTnt tnt = new PrimedTnt(player.level(), player.getX(), player.getY(), player.getZ(), player);
                    player.level().addFreshEntity(tnt);
                    activeTnt++;
                }
                ticks = 0;
            }
            timers.put(uuid, ticks);
        }
    }
}
