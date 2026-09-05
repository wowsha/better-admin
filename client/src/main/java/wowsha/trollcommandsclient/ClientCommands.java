package wowsha.trollcommandsclient;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;

public final class ClientCommands {
    private static final Random RANDOM = new Random();
    private static final List<String> LOOT = List.of(
            "minecraft:diamond",
            "minecraft:netherite_ingot",
            "minecraft:netherite_block",
            "minecraft:diamond_block",
            "minecraft:beacon",
            "minecraft:barrier",
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:enchanted_golden_apple",
            "minecraft:totem_of_undying",
            "minecraft:elytra"
    );

    private static boolean tntEnabled;
    private static boolean itemRainEnabled;
    private static boolean vanished;
    private static int tntTimer;
    private static int itemRainTimer;

    private ClientCommands() {}

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("lava")
                .executes(context -> lava()));

        dispatcher.register(Commands.literal("warden")
                .executes(context -> warden()));

        dispatcher.register(Commands.literal("creeper")
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> creeper(StringArgumentType.getString(context, "player")))));

        dispatcher.register(Commands.literal("rich")
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> rich(StringArgumentType.getString(context, "player")))));

        dispatcher.register(Commands.literal("tnt")
                .executes(context -> toggleTnt(false))
                .then(Commands.literal("stop")
                        .executes(context -> toggleTnt(true))));

        dispatcher.register(Commands.literal("vanish")
                .executes(context -> vanish(false))
                .then(Commands.literal("stop")
                        .executes(context -> vanish(true))));

        dispatcher.register(Commands.literal("itemrain")
                .executes(context -> toggleItemRain(false))
                .then(Commands.literal("stop")
                        .executes(context -> toggleItemRain(true))));

        dispatcher.register(Commands.literal("restart")
                .executes(context -> restart()));
    }

    static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        if (!minecraft.player.hasPermissions(2)) return;

        if (tntEnabled) {
            if (++tntTimer >= 80) {
                tntTimer = 0;
                send("summon tnt ~ ~ ~ {Fuse:80}");
            }
        } else {
            tntTimer = 0;
        }

        if (itemRainEnabled) {
            if (++itemRainTimer >= 20) {
                itemRainTimer = 0;
                for (int i = 0; i < 6; i++) {
                    int dx = RANDOM.nextInt(16) - 8;
                    int dz = RANDOM.nextInt(16) - 8;
                    String item = LOOT.get(RANDOM.nextInt(LOOT.size()));
                    send("execute as @a at @s run summon item ~" + dx + " ~1 ~" + dz
                            + " {Item:{id:\"" + item + "\",Count:1b},PickupDelay:10}");
                }
            }
        } else {
            itemRainTimer = 0;
        }
    }

    private static int lava() {
        if (!allowed()) return 0;
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = minecraft.player.blockPosition();
        int chunkMinX = pos.getX() >> 4 << 4;
        int chunkMinZ = pos.getZ() >> 4 << 4;
        int y = minecraft.level.dimension().location().getPath().equals("the_nether") ? 127 : 319;
        send("fill " + chunkMinX + " " + y + " " + chunkMinZ + " "
                + (chunkMinX + 15) + " " + y + " " + (chunkMinZ + 15) + " lava replace air");
        return 1;
    }

    private static int warden() {
        if (!allowed()) return 0;
        String[] offsets = {"2", "-2"};
        for (String x : offsets) {
            for (String z : offsets) {
                send("execute as @a at @s run summon warden " + x + " ~ " + z);
            }
        }
        send("execute as @a at @s run summon warden 0 ~ 3");
        return 1;
    }

    private static int creeper(String target) {
        if (!allowed()) return 0;
        send("execute at " + target + " run summon creeper ~5 ~ ~ "
                + "{Attributes:[{Name:\"minecraft:generic.max_health\",Base:300.0}],Health:300.0f}");
        return 1;
    }

    private static int rich(String target) {
        if (!allowed()) return 0;
        send("item replace entity " + target + " armor.head with minecraft:netherite_helmet");
        send("item replace entity " + target + " armor.chest with minecraft:netherite_chestplate");
        send("item replace entity " + target + " armor.legs with minecraft:netherite_leggings");
        send("item replace entity " + target + " armor.feet with minecraft:netherite_boots");
        send("item replace entity " + target + " weapon.offhand with minecraft:totem_of_undying");
        send("give " + target + " minecraft:totem_of_undying 2");
        send("give " + target + " minecraft:netherite_sword");
        send("give " + target + " minecraft:netherite_pickaxe");
        send("give " + target + " minecraft:netherite_axe");
        send("give " + target + " minecraft:netherite_shovel");
        send("give " + target + " minecraft:netherite_hoe");
        return 1;
    }

    private static int toggleTnt(boolean stop) {
        if (!allowed()) return 0;
        tntEnabled = !stop && !tntEnabled;
        tntTimer = 0;
        return 1;
    }

    private static int vanish(boolean stop) {
        if (!allowed()) return 0;
        if (stop || vanished) {
            send("effect clear @s minecraft:invisibility");
            vanished = false;
        } else {
            send("effect give @s minecraft:invisibility 1000000 0 true");
            vanished = true;
        }
        return 1;
    }

    private static int toggleItemRain(boolean stop) {
        if (!allowed()) return 0;
        itemRainEnabled = !stop && !itemRainEnabled;
        itemRainTimer = 0;
        return 1;
    }

    private static int restart() {
        if (!allowed()) return 0;
        send("save-all flush");
        send("stop");
        return 1;
    }

    private static boolean allowed() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.getConnection() != null && minecraft.player.hasPermissions(2);
    }

    private static void send(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) connection.sendCommand(command);
    }
}
