package wowsha.betteradmin;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import wowsha.betteradmin.command.AdminCommands;
import wowsha.betteradmin.util.MinerManager;
import wowsha.betteradmin.util.OreManager;
import wowsha.betteradmin.util.TntManager;
import wowsha.betteradmin.util.VanishManager;

@Mod(BetterAdmin.MOD_ID)
public class BetterAdmin {
    public static final String MOD_ID = "troll_commands";

    public BetterAdmin() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(TntManager.class);
        MinecraftForge.EVENT_BUS.register(VanishManager.class);
        MinecraftForge.EVENT_BUS.register(AdminCommands.class);
        MinecraftForge.EVENT_BUS.register(MinerManager.class);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        AdminCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        TntManager.stopAll();
        VanishManager.clear();
        MinerManager.stopAll();
        OreManager.clear();
    }
}
