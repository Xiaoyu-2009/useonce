package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nonamecrackers2.witherstormmod.WitherStormMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    // 凋灵风暴模组中无耐久的功能道具（相位仪）在结束一次使用后消耗
    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void useonce$consumeWitherStormFunctionalItem(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide || !self.isUsingItem()) {
            return;
        }
        UseOnceConfig config = UseOnceConfig.get();
        if (!config.singleUseItems) {
            return;
        }
        if (config.creativeExempt && self instanceof Player player && player.getAbilities().instabuild) {
            return;
        }
        ItemStack stack = self.getUseItem();
        if (stack.isEmpty() || stack.isDamageableItem() || stack.has(DataComponents.FOOD)) {
            return;
        }
        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(WitherStormMod.MOD_ID)) {
            stack.shrink(1);
        }
    }
}
