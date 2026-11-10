package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    // 将物品耐久上限限制为 1，实现一次性使用
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void useonce$clampMaxDamage(CallbackInfoReturnable<Integer> cir) {
        UseOnceConfig config = UseOnceConfig.get();
        if (!config.singleUseItems) {
            return;
        }
        if (config.fishingRodExempt && ((ItemStack) (Object) this).getItem() instanceof FishingRodItem) {
            return;
        }
        if (cir.getReturnValueI() > 0) {
            cir.setReturnValue(1);
        }
    }
}
