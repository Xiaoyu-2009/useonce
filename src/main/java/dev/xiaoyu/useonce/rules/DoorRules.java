package dev.xiaoyu.useonce.rules;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class DoorRules {
    private DoorRules() {
    }

    // 门被使用后延时破坏（双格门只破坏下半部分）
    public static void breakAfterUse(ServerLevel level, BlockPos pos) {
        BlockPos clicked = pos.immutable();
        ServerTasks.defer(level, () -> {
            BlockPos target = clicked;
            BlockState state = level.getBlockState(clicked);
            if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                target = clicked.below();
                state = level.getBlockState(target);
            }

            if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock || state.getBlock() instanceof FenceGateBlock) {
                level.playSound(null, target, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
                level.destroyBlock(target, false);
            }
        });
    }
}
