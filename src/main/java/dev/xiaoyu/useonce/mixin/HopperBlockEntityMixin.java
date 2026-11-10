package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import dev.xiaoyu.useonce.rules.ContainerRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {
    // 漏斗不能从已作废的容器中取物
    @Inject(method = "tryTakeInItemFromSlot", at = @At("HEAD"), cancellable = true)
    private static void useonce$refuseDoomedSource(Hopper hopper, Container container, int slot, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (UseOnceConfig.get().containerHopperExtraction && ContainerRules.isInScope(container) && ContainerRules.isDoomed(container)) {
            cir.setReturnValue(false);
        }
    }

    // 漏斗取走物品后让源容器损坏
    @Inject(method = "tryTakeInItemFromSlot", at = @At("RETURN"))
    private static void useonce$breakDrainedSource(Hopper hopper, Container container, int slot, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && UseOnceConfig.get().containerHopperExtraction && ContainerRules.isInScope(container)) {
            ContainerRules.breakContainer(container);
        }
    }

    // 已作废的漏斗不再向外推送
    @Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
    private static void useonce$refuseDoomedEjector(Level level, BlockPos pos, HopperBlockEntity self, CallbackInfoReturnable<Boolean> cir) {
        if (UseOnceConfig.get().containerPushExtraction && ContainerRules.isInScope(self) && ContainerRules.isDoomed(self)) {
            cir.setReturnValue(false);
        }
    }

    // 漏斗推出物品后自身损坏
    @Inject(method = "ejectItems", at = @At("RETURN"))
    private static void useonce$breakDrainedEjector(Level level, BlockPos pos, HopperBlockEntity self, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && UseOnceConfig.get().containerPushExtraction && ContainerRules.isInScope(self)) {
            ContainerRules.breakContainer(self);
        }
    }
}
