package wowsha.trollcommandsclient;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(TrollCommandsClient.MOD_ID)
public final class TrollCommandsClient {
    public static final String MOD_ID = "troll_commands_client";

    public TrollCommandsClient() {
        MinecraftForge.EVENT_BUS.register(ClientCommands.class);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientCommands.tick();
    }
}
