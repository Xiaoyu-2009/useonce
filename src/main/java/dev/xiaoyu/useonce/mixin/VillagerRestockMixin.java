package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.rules.TradeRules;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerRestockMixin {
    // 禁用村民补货
    @Inject(method = "restock", at = @At("HEAD"), cancellable = true)
    private void useonce$blockRestock(CallbackInfo ci) {
        if (TradeRules.blocksRestock()) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldRestock", at = @At("HEAD"), cancellable = true)
    private void useonce$blockRestockCheck(CallbackInfoReturnable<Boolean> cir) {
        if (TradeRules.blocksRestock()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canRestock", at = @At("HEAD"), cancellable = true)
    private void useonce$reportNoRestock(CallbackInfoReturnable<Boolean> cir) {
        if (TradeRules.blocksRestock()) {
            cir.setReturnValue(false);
        }
    }
}
