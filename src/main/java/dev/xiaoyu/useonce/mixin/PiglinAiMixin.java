package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import dev.xiaoyu.useonce.rules.ServerTasks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinAi.class)
public class PiglinAiMixin {
    // 猪灵以物易物后死亡
    @Inject(
            method = "stopHoldingOffHandItem(Lnet/minecraft/world/entity/monster/piglin/Piglin;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;throwItems(Lnet/minecraft/world/entity/monster/piglin/Piglin;Ljava/util/List;)V",
                    ordinal = 0,
                    shift = Shift.AFTER
            )
    )
    private static void useonce$killAfterBarter(Piglin body, boolean barteringEnabled, CallbackInfo ci) {
        if (UseOnceConfig.get().piglinBarterOnce) {
            Level level = body.level();
            if (level instanceof ServerLevel serverLevel) {
                ServerTasks.defer(serverLevel, () -> {
                    if (!body.isRemoved() && body.isAlive() && body.level() instanceof ServerLevel) {
                        body.kill();
                    }
                });
            }
        }
    }
}
