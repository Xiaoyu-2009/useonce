package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.rules.ContainerRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    @Final
    protected ServerPlayer player;

    // 记录正在破坏方块的玩家
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void useonce$beginPlayerBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ContainerRules.beginPlayerBreak(player);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void useonce$endPlayerBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ContainerRules.endPlayerBreak();
    }
}
