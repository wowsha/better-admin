package wowsha.betteradmin.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class VanishManager {
    private static final Set<UUID> VANISHED = new HashSet<>();

    private VanishManager() {}

    public static void toggle(ServerPlayer player) {
        if (VANISHED.remove(player.getUUID())) {
            player.setInvisible(false);
            broadcastVisible(player);
        } else {
            VANISHED.add(player.getUUID());
            player.setInvisible(true);
            broadcastHidden(player);
        }
    }

    public static boolean isVanished(ServerPlayer player) {
        return VANISHED.contains(player.getUUID());
    }

    public static void clear() {
        VANISHED.clear();
    }

    private static Component vanillaStatus(String name, String action) {
        return Component.literal(name + " " + action)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
    }

    private static void broadcastHidden(ServerPlayer vanished) {
        var server = vanished.getServer();
        if (server == null) return;
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer != vanished) {
                viewer.connection.send(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(vanished.getUUID())));
            }
        }
        server.getPlayerList().broadcastSystemMessage(
                vanillaStatus(vanished.getGameProfile().getName(), "left the game"), false);
    }

    private static void broadcastVisible(ServerPlayer vanished) {
        var server = vanished.getServer();
        if (server == null) return;
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer != vanished) {
                viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singletonList(vanished)));
            }
        }
        server.getPlayerList().broadcastSystemMessage(
                vanillaStatus(vanished.getGameProfile().getName(), "joined the game"), false);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining)) return;
        var server = joining.getServer();
        if (server == null) return;
        for (UUID uuid : VANISHED) {
            ServerPlayer hidden = server.getPlayerList().getPlayer(uuid);
            if (hidden != null && hidden != joining) {
                joining.connection.send(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(hidden.getUUID())));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        VANISHED.remove(event.getEntity().getUUID());
    }
}
