package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import dev.xiaoyu.useonce.rules.ServerTasks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private boolean useonce$crossDimensionEject;

    // 标记跨维度传送过程，避免误判乘客离开
    @Inject(method = "changeDimension", at = @At("HEAD"))
    private void useonce$enterCrossDimension(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        useonce$crossDimensionEject = true;
    }

    @Inject(method = "changeDimension", at = @At("RETURN"))
    private void useonce$exitCrossDimension(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        useonce$crossDimensionEject = false;
    }

    // 乘客下车后船也会损坏
    @Inject(method = "removePassenger", at = @At("TAIL"))
    private void useonce$breakBoatOnExit(Entity passenger, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Boat boat)) {
            return;
        }
        UseOnceConfig config = UseOnceConfig.get();
        if (!config.singleUseBoats || !(boat.level() instanceof ServerLevel serverLevel) || boat.isRemoved()) {
            return;
        }
        if (!boat.getPassengers().isEmpty() || useonce$crossDimensionEject) {
            return;
        }
        if (passenger instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
            return;
        }
        Entity.RemovalReason removalReason = passenger.getRemovalReason();
        if (removalReason != null) {
            if (!removalReason.shouldDestroy() || passenger instanceof Player) {
                return;
            }
        }
        if (config.creativeExempt && passenger instanceof Player player && player.getAbilities().instabuild) {
            return;
        }
        ServerTasks.defer(serverLevel, () -> {
            if (!boat.isRemoved() && boat.level() == serverLevel && boat.getPassengers().isEmpty()) {
                serverLevel.playSound(null, boat.getX(), boat.getY(), boat.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
                boat.discard();
            }
        });
    }
}
