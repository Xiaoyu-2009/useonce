package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nonamecrackers2.witherstormmod.common.item.AmuletItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 凋灵风暴：护身符在切换追踪目标或绑定实体后一次性消耗
@Mixin(value = AmuletItem.class, remap = false)
public class AmuletItemMixin {
    @Inject(method = "use", at = @At("RETURN"))
    private void useonce$consumeOnSwitch(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide || !player.isShiftKeyDown()) {
            return;
        }
        UseOnceConfig config = UseOnceConfig.get();
        if (!config.singleUseItems) {
            return;
        }
        if (config.creativeExempt && player.getAbilities().instabuild) {
            return;
        }
        player.getItemInHand(hand).shrink(1);
    }

    @Inject(method = "interactLivingEntity", at = @At("RETURN"))
    private void useonce$consumeOnBind(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.level().isClientSide || cir.getReturnValue() == InteractionResult.PASS) {
            return;
        }
        UseOnceConfig config = UseOnceConfig.get();
        if (!config.singleUseItems) {
            return;
        }
        if (config.creativeExempt && player.getAbilities().instabuild) {
            return;
        }
        stack.shrink(1);
    }
}
