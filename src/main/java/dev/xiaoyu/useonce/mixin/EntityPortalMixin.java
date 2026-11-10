package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.rules.PortalRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPortalMixin {
    @Unique
    private ServerLevel useonce$portalOriginLevel;
    @Unique
    private BlockPos useonce$portalEntryPos;

    // 传送前记录来源维度与进入的传送门位置
    @Inject(method = "handlePortal", at = @At("HEAD"))
    private void useonce$capturePortalOrigin(CallbackInfo ci) {
        useonce$portalOriginLevel = null;
        useonce$portalEntryPos = null;
        Entity self = (Entity) (Object) this;
        PortalProcessor process = self.portalProcess;
        if (process != null && self.level() instanceof ServerLevel origin) {
            useonce$portalOriginLevel = origin;
            useonce$portalEntryPos = process.getEntryPosition();
        }
    }

    // 传送发生时让原传送门熄灭
    @Inject(
            method = "handlePortal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;"
            )
    )
    private void useonce$burnOutOriginPortal(CallbackInfo ci) {
        ServerLevel origin = useonce$portalOriginLevel;
        BlockPos entryPos = useonce$portalEntryPos;
        useonce$portalOriginLevel = null;
        useonce$portalEntryPos = null;
        if (origin != null && entryPos != null) {
            PortalRules.onPortalTravel(origin, entryPos, (Entity) (Object) this);
        }
    }
}
