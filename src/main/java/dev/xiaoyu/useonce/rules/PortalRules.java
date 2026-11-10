package dev.xiaoyu.useonce.rules;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class PortalRules {
    private static final int MAX_NETHER_PORTAL_BLOCKS = 441;
    private static final int MAX_END_PORTAL_BLOCKS = 256;
    private static final Direction[] NEIGHBOURS = Direction.values();

    private PortalRules() {
    }

    // 实体穿过传送门后让原传送门熄灭
    public static void onPortalTravel(ServerLevel origin, BlockPos entryPos, Entity traveller) {
        UseOnceConfig config = UseOnceConfig.get();
        Player player = travellingPlayer(traveller);
        if (!config.portalsPlayersOnly || player != null) {
            if (!config.creativeExempt || player == null || !player.getAbilities().instabuild) {
                if (origin.isLoaded(entryPos)) {
                    Block block = origin.getBlockState(entryPos).getBlock();
                    if (block instanceof NetherPortalBlock) {
                        if (config.netherPortalSingleUse) {
                            ServerTasks.defer(origin, () -> extinguishNetherPortal(origin, entryPos));
                        }
                    } else if (block instanceof EndPortalBlock && config.endPortalSingleUse) {
                        ServerTasks.defer(origin, () -> collapseEndPortal(origin, entryPos));
                    }
                }
            }
        }
    }

    // 玩家离开末地传送门时让传送门崩塌
    public static void onEndCreditsExit(ServerLevel end, BlockPos portalPos, Player player) {
        UseOnceConfig config = UseOnceConfig.get();
        if (config.endPortalSingleUse) {
            if (!config.creativeExempt || !player.getAbilities().instabuild) {
                ServerTasks.defer(end, () -> collapseEndPortal(end, portalPos));
            }
        }
    }

    public static void extinguishNetherPortal(ServerLevel level, BlockPos origin) {
        List<BlockPos> portal = collectConnected(level, origin, state -> state.getBlock() instanceof NetherPortalBlock, MAX_NETHER_PORTAL_BLOCKS);
        if (!portal.isEmpty()) {
            for (BlockPos pos : portal) {
                if (level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof NetherPortalBlock) {
                    level.removeBlock(pos, false);
                }
            }

            level.playSound(null, origin, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            puff(level, portal, ParticleTypes.LARGE_SMOKE);
        }
    }

    public static void collapseEndPortal(ServerLevel level, BlockPos origin) {
        List<BlockPos> portal = collectConnected(level, origin, state -> state.getBlock() instanceof EndPortalBlock, MAX_END_PORTAL_BLOCKS);
        if (!portal.isEmpty()) {
            Set<BlockPos> portalBlocks = new HashSet<>(portal);
            Set<BlockPos> checked = new HashSet<>();
            boolean clearedAnyEye = false;

            // 取下末地传送门框架上的末影之眼
            for (BlockPos pos : portal) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) {
                                continue;
                            }
                            BlockPos framePos = pos.offset(dx, dy, dz);
                            if (portalBlocks.contains(framePos) || !checked.add(framePos) || !level.isLoaded(framePos)) {
                                continue;
                            }
                            BlockState frameState = level.getBlockState(framePos);
                            if (frameState.getBlock() instanceof EndPortalFrameBlock && frameState.getValue(EndPortalFrameBlock.HAS_EYE)) {
                                level.setBlock(framePos, frameState.setValue(EndPortalFrameBlock.HAS_EYE, false), 3);
                                clearedAnyEye = true;
                            }
                        }
                    }
                }
            }

            for (BlockPos pos : portal) {
                if (level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof EndPortalBlock) {
                    level.removeBlock(pos, false);
                }
            }

            level.playSound(null, origin, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8F, 0.5F);
            if (clearedAnyEye) {
                level.playSound(null, origin, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 1.0F, 0.7F);
            }
            puff(level, portal, ParticleTypes.REVERSE_PORTAL);
        }
    }

    // 找出穿越传送门的玩家（包括末影珍珠主人在内的乘客）
    private static Player travellingPlayer(Entity traveller) {
        if (traveller instanceof Player player) {
            return player;
        }
        if (traveller instanceof ThrownEnderpearl pearl && pearl.getOwner() instanceof Player owner) {
            return owner;
        }
        for (Entity passenger : traveller.getIndirectPassengers()) {
            if (passenger instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    // 收集相连的传送门方块
    private static List<BlockPos> collectConnected(ServerLevel level, BlockPos origin, Predicate<BlockState> matches, int limit) {
        if (!level.isLoaded(origin) || !matches.test(level.getBlockState(origin))) {
            return List.of();
        }

        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin);
        queue.addLast(origin);

        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos pos = queue.removeFirst();
            found.add(pos);
            for (Direction direction : NEIGHBOURS) {
                BlockPos next = pos.relative(direction);
                if (seen.add(next) && level.isLoaded(next) && matches.test(level.getBlockState(next))) {
                    queue.addLast(next);
                }
            }
        }
        return found;
    }

    // 在传送门中心区域生成粒子
    private static void puff(ServerLevel level, List<BlockPos> positions, ParticleOptions particle) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        double centreX = (double) (minX + maxX) / 2.0 + 0.5;
        double centreY = (double) (minY + maxY) / 2.0 + 0.5;
        double centreZ = (double) (minZ + maxZ) / 2.0 + 0.5;
        double spreadX = (double) (maxX - minX) / 4.0 + 0.25;
        double spreadY = (double) (maxY - minY) / 4.0 + 0.25;
        double spreadZ = (double) (maxZ - minZ) / 4.0 + 0.25;
        level.sendParticles(particle, centreX, centreY, centreZ, Math.min(positions.size() * 3, 96), spreadX, spreadY, spreadZ, 0.0);
    }
}
