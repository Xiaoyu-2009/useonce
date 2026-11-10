package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class BucketItemMixin {
    // 使用一次桶后不再返还空桶
    @Inject(method = "getEmptySuccessItem", at = @At("RETURN"), cancellable = true)
    private static void useonce$consumeBucket(ItemStack original, Player player, CallbackInfoReturnable<ItemStack> cir) {
        UseOnceConfig config = UseOnceConfig.get();
        if (!config.singleUseBuckets) {
            return;
        }
        if (player != null && config.creativeExempt && player.getAbilities().instabuild) {
            return;
        }
        if (player != null && !player.level().isClientSide()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
        }
        cir.setReturnValue(ItemStack.EMPTY);
    }
}
