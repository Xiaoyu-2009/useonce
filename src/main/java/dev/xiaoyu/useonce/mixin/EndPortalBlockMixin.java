package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.rules.PortalRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockMixin {
    // 玩家从末地传送门返回后传送门崩塌
    @Inject(
            method = "entityInside",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;showEndCredits()V"
            )
    )
    private void useonce$collapseAfterExit(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
            PortalRules.onEndCreditsExit(serverLevel, pos, player);
        }
    }
}
