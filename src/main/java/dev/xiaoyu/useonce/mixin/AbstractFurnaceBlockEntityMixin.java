package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import dev.xiaoyu.useonce.duck.FurnaceResultTracker;
import dev.xiaoyu.useonce.rules.ContainerRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin implements FurnaceResultTracker {
    @Unique
    private int useonce$resultCountBefore;

    @Override
    public int useonce$getResultCountBefore() {
        return useonce$resultCountBefore;
    }

    @Override
    public void useonce$setResultCountBefore(int count) {
        useonce$resultCountBefore = count;
    }

    // 记录熔炼前产物数量
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void useonce$captureResultCount(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        ((FurnaceResultTracker) blockEntity).useonce$setResultCountBefore(blockEntity.getItem(2).getCount());
    }

    // 产出一次后破坏熔炉
    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void useonce$breakAfterSmelt(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        if (!UseOnceConfig.get().singleUseFurnaces) {
            return;
        }
        int before = ((FurnaceResultTracker) blockEntity).useonce$getResultCountBefore();
        int after = blockEntity.getItem(2).getCount();
        if (after > before && level instanceof ServerLevel serverLevel) {
            ContainerRules.dropContentsUnforfeited(serverLevel, pos, blockEntity);
            serverLevel.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
            serverLevel.destroyBlock(pos, false);
        }
    }
}
