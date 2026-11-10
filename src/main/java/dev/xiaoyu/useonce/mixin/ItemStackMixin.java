package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import nonamecrackers2.witherstormmod.WitherStormMod;
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

    // 凋灵风暴模组的装备带有“无敌”组件，这里强制其可损坏，配合耐久上限 1 实现一次性使用
    @Inject(method = "isDamageableItem", at = @At("RETURN"), cancellable = true)
    private void useonce$forceWitherStormDamageable(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        if (!UseOnceConfig.get().singleUseItems) {
            return;
        }
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.has(DataComponents.MAX_DAMAGE) || !stack.has(DataComponents.DAMAGE)) {
            return;
        }
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(WitherStormMod.MOD_ID)) {
            return;
        }
        cir.setReturnValue(true);
    }
}
