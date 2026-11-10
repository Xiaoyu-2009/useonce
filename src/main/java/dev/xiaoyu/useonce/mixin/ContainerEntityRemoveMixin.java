package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.rules.ContainerRules;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractMinecartContainer.class, ChestBoat.class})
public abstract class ContainerEntityRemoveMixin {
    // 容器实体被销毁时内容物作废
    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void useonce$forfeitCargoOnDestroy(Entity.RemovalReason reason, CallbackInfo ci) {
        if (reason.shouldDestroy()) {
            Entity self = (Entity) (Object) this;
            ContainerRules.forfeitOnBreak(self.level(), (Container) this);
        }
    }
}
