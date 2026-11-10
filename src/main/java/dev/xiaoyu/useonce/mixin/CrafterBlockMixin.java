package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import dev.xiaoyu.useonce.rules.ContainerRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {
    // 合成一次后破坏合成器
    @Inject(
            method = "dispenseFrom",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/CrafterBlockEntity;setChanged()V",
                    shift = Shift.AFTER
            ),
            cancellable = true
    )
    private void useonce$breakAfterCraft(BlockState state, ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (UseOnceConfig.get().singleUseCrafters && level.getBlockEntity(pos) instanceof CrafterBlockEntity blockEntity) {
            ContainerRules.dropContentsUnforfeited(level, pos, blockEntity);
            level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
            level.destroyBlock(pos, false);
            ci.cancel();
        }
    }
}
