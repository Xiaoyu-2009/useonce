package dev.xiaoyu.useonce.mixin;

import com.mojang.datafixers.util.Either;
import dev.xiaoyu.useonce.config.UseOnceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Unique
    private boolean useonce$spawnJustSet;
    @Unique
    private BlockPos useonce$occupiedBedPos;

    // 设置重生点时记录标志（入睡流程中调用）
    @Inject(method = "setRespawnPosition", at = @At("HEAD"))
    private void useonce$flagSpawnSet(ResourceKey<Level> dimension, BlockPos pos, float angle, boolean forced, boolean sendMessage, CallbackInfo ci) {
        useonce$spawnJustSet = true;
    }

    // 睡觉后记录床的位置，无法入睡时直接破坏床
    @Inject(method = "startSleepInBed", at = @At("RETURN"))
    private void useonce$breakBedAfterUse(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        boolean spawnSet = useonce$spawnJustSet;
        useonce$spawnJustSet = false;
        UseOnceConfig config = UseOnceConfig.get();
        if (!config.singleUseBeds) {
            return;
        }
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (config.creativeExempt && self.getAbilities().instabuild) {
            return;
        }
        if (cir.getReturnValue().right().isPresent()) {
            useonce$occupiedBedPos = pos;
        } else if (spawnSet) {
            useonce$destroyBed(self, pos);
        }
    }

    // 起床后破坏床
    @Inject(method = "stopSleepInBed", at = @At("TAIL"))
    private void useonce$breakBedOnWake(boolean forcefulWakeUp, boolean updateLevelList, CallbackInfo ci) {
        BlockPos pos = useonce$occupiedBedPos;
        if (pos != null) {
            useonce$occupiedBedPos = null;
            if (UseOnceConfig.get().singleUseBeds) {
                useonce$destroyBed((ServerPlayer) (Object) this, pos);
            }
        }
    }

    @Unique
    private void useonce$destroyBed(ServerPlayer player, BlockPos pos) {
        if (player.level().getBlockState(pos).getBlock() instanceof BedBlock) {
            player.level().destroyBlock(pos, false);
        }
    }
}
