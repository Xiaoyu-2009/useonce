package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import dev.xiaoyu.useonce.rules.DoorRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({DoorBlock.class, TrapDoorBlock.class, FenceGateBlock.class})
public abstract class DoorLikeBlockMixin {
    // 门类方块使用一次后破坏
    @Inject(method = "useWithoutItem", at = @At("RETURN"))
    private void useonce$breakAfterUse(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!UseOnceConfig.get().singleUseDoors || !cir.getReturnValue().consumesAction()) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            if (UseOnceConfig.get().creativeExempt && player.getAbilities().instabuild) {
                return;
            }
            DoorRules.breakAfterUse(serverLevel, pos);
        }
    }
}
