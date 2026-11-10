package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.rules.ContainerRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Containers.class)
public class ContainersMixin {
    // 方块容器被破坏时内容物作废
    @Inject(method = "dropContents(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/Container;)V", at = @At("HEAD"))
    private static void useonce$forfeitBlockContentsOnBreak(Level level, BlockPos pos, Container container, CallbackInfo ci) {
        if (!ContainerRules.selfDrop) {
            ContainerRules.forfeitOnBreak(level, container);
        }
    }

    // 实体容器被销毁时内容物作废
    @Inject(method = "dropContents(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/Container;)V", at = @At("HEAD"))
    private static void useonce$forfeitEntityContentsOnBreak(Level level, Entity entity, Container container, CallbackInfo ci) {
        if (!ContainerRules.selfDrop) {
            ContainerRules.forfeitOnBreak(level, container);
        }
    }
}
