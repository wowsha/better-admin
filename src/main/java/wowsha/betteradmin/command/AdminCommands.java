package wowsha.betteradmin.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import wowsha.betteradmin.util.MinerManager;
import wowsha.betteradmin.util.OreManager;
import wowsha.betteradmin.util.TntManager;
import wowsha.betteradmin.util.VanishManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdminCommands {
    private static final Map<UUID, UUID> HUNTER_TARGETS = new HashMap<>();
    private static final Map<UUID, Integer> HUNTER_BREAK_COOLDOWNS = new HashMap<>();

    private AdminCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lava")
                .executes(context -> lava(context.getSource())));

        dispatcher.register(Commands.literal("warden")
                .executes(context -> warden(context.getSource())));

        dispatcher.register(Commands.literal("creeper")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> creeper(EntityArgument.getPlayer(context, "player")))));

        dispatcher.register(Commands.literal("rich")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> rich(EntityArgument.getPlayer(context, "player")))));

        dispatcher.register(Commands.literal("tnt")
                .executes(context -> tnt(context.getSource(), false))
                .then(Commands.literal("stop")
                        .executes(context -> tnt(context.getSource(), true))));

        dispatcher.register(Commands.literal("vanish")
                .executes(context -> vanish(context.getSource())));

        dispatcher.register(Commands.literal("miner")
                .executes(context -> miner(context.getSource(), false))
                .then(Commands.literal("stop")
                        .executes(context -> miner(context.getSource(), true))));

        dispatcher.register(Commands.literal("ores")
                .executes(context -> ores(context.getSource(), false))
                .then(Commands.literal("regenerate")
                        .executes(context -> ores(context.getSource(), true))));
    }

    private static int lava(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        int y = level.getMaxBuildHeight() - 1;
        int minX = origin.getX() >> 4 << 4;
        int minZ = origin.getZ() >> 4 << 4;

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos).canBeReplaced()) {
                    level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
                }
            }
        }
        return 1;
    }

    private static int warden(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer executor = source.getPlayerOrException();
        for (ServerPlayer target : executor.server.getPlayerList().getPlayers()) {
            int spawned = 0;
            for (int i = 0; i < 5 && spawned < 5; i++) {
                Warden warden = EntityType.WARDEN.create(target.serverLevel());
                if (warden == null) continue;
                BlockPos base = target.blockPosition().offset((i % 2 == 0 ? 1 : -1) * (2 + i), 0, (i % 3) - 1);
                if (!target.serverLevel().noCollision(warden, warden.getBoundingBox().move(base.getX() + 0.5, base.getY(), base.getZ() + 0.5))) {
                    continue;
                }
                warden.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, target.getYRot(), 0);
                warden.setTarget(target);
                target.serverLevel().addFreshEntity(warden);
                spawned++;
            }
        }
        return 1;
    }

    private static int creeper(ServerPlayer target) {
        ServerLevel level = target.serverLevel();
        BlockPos spawn = findHiddenSpawn(level, target);
        Creeper creeper = EntityType.CREEPER.create(level);
        if (creeper == null) return 0;

        double maxHealth = 300.0D;
        var attribute = creeper.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (attribute != null) attribute.setBaseValue(maxHealth);
        creeper.setHealth((float) maxHealth);
        creeper.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, target.getYRot(), 0);
        creeper.setTarget(target);
        level.addFreshEntity(creeper);
        HUNTER_TARGETS.put(creeper.getUUID(), target.getUUID());
        HUNTER_BREAK_COOLDOWNS.put(creeper.getUUID(), 0);
        return 1;
    }

    private static BlockPos findHiddenSpawn(ServerLevel level, ServerPlayer target) {
        BlockPos center = target.blockPosition();
        for (int radius = 5; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    BlockPos feet = center.offset(dx, 0, dz);
                    if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) continue;
                    if (!level.getBlockState(feet.below()).isSolid()) continue;
                    var hit = level.clip(new ClipContext(
                            feet.getCenter().add(0, 0.8, 0),
                            target.position().add(0, 1.0, 0),
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            target));
                    if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) return feet;
                }
            }
        }
        return center.relative(Direction.NORTH, 6);
    }

    private static int rich(ServerPlayer player) {
        giveArmor(player, 0, Items.NETHERITE_BOOTS.getDefaultInstance());
        giveArmor(player, 1, Items.NETHERITE_LEGGINGS.getDefaultInstance());
        giveArmor(player, 2, Items.NETHERITE_CHESTPLATE.getDefaultInstance());
        giveArmor(player, 3, Items.NETHERITE_HELMET.getDefaultInstance());

        ItemStack oldOffhand = player.getInventory().offhand.get(0);
        if (!oldOffhand.isEmpty()) player.getInventory().placeItemBackInInventory(oldOffhand.copy());
        player.getInventory().offhand.set(0, Items.TOTEM_OF_UNDYING.getDefaultInstance());
        player.getInventory().add(Items.TOTEM_OF_UNDYING.getDefaultInstance());
        player.getInventory().add(Items.TOTEM_OF_UNDYING.getDefaultInstance());

        player.getInventory().add(Items.NETHERITE_SWORD.getDefaultInstance());
        player.getInventory().add(Items.NETHERITE_PICKAXE.getDefaultInstance());
        player.getInventory().add(Items.NETHERITE_AXE.getDefaultInstance());
        player.getInventory().add(Items.NETHERITE_SHOVEL.getDefaultInstance());
        player.getInventory().add(Items.NETHERITE_HOE.getDefaultInstance());
        player.inventoryMenu.broadcastChanges();
        return 1;
    }

    private static void giveArmor(ServerPlayer player, int slot, ItemStack replacement) {
        ItemStack old = player.getInventory().armor.get(slot);
        if (!old.isEmpty()) player.getInventory().placeItemBackInInventory(old.copy());
        player.getInventory().armor.set(slot, replacement);
    }

    private static int tnt(CommandSourceStack source, boolean stop) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (stop) TntManager.stop(player); else TntManager.toggle(player);
        return 1;
    }

    private static int vanish(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        VanishManager.toggle(source.getPlayerOrException());
        return 1;
    }

    private static int miner(CommandSourceStack source, boolean stop) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (stop) MinerManager.stopAll(); else MinerManager.spawn(player);
        return 1;
    }

    private static int ores(CommandSourceStack source, boolean regenerate) {
        if (regenerate) OreManager.regenerate();
        else OreManager.removeActiveOres(source.getServer());
        return 1;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || HUNTER_TARGETS.isEmpty()) return;

        for (UUID creeperId : List.copyOf(HUNTER_TARGETS.keySet())) {
            Entity entity = findEntity(server, creeperId);
            UUID targetId = HUNTER_TARGETS.get(creeperId);
            if (!(entity instanceof Creeper creeper)) {
                HUNTER_TARGETS.remove(creeperId);
                HUNTER_BREAK_COOLDOWNS.remove(creeperId);
                continue;
            }
            ServerPlayer target = server.getPlayerList().getPlayer(targetId);
            if (target == null || !target.isAlive() || creeper.isRemoved()) {
                HUNTER_TARGETS.remove(creeperId);
                HUNTER_BREAK_COOLDOWNS.remove(creeperId);
                continue;
            }

            creeper.setTarget(target);
            int cooldown = HUNTER_BREAK_COOLDOWNS.getOrDefault(creeperId, 0);
            if (cooldown > 0) {
                HUNTER_BREAK_COOLDOWNS.put(creeperId, cooldown - 1);
            } else if (!creeper.hasLineOfSight(target) && creeper.distanceTo(target) < 12.0F) {
                BlockPos ahead = BlockPos.containing(creeper.position().add(target.position().subtract(creeper.position()).normalize().scale(1.2)));
                BlockState state = creeper.level().getBlockState(ahead);
                if (!state.isAir() && state.getDestroySpeed(creeper.level(), ahead) >= 0 && state.getDestroySpeed(creeper.level(), ahead) <= 20.0F) {
                    creeper.level().destroyBlock(ahead, true, creeper);
                    HUNTER_BREAK_COOLDOWNS.put(creeperId, 10);
                }
            }
        }
    }

    private static Entity findEntity(net.minecraft.server.MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity found = level.getEntity(id);
            if (found != null) return found;
        }
        return null;
    }
}
