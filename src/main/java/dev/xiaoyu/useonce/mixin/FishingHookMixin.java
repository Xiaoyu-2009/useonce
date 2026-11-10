package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Shadow
    private int nibble;

    // 钓到物品后消耗鱼竿
    @Inject(method = "retrieve", at = @At("RETURN"))
    private void useonce$breakRodAfterCatch(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        UseOnceConfig config = UseOnceConfig.get();
        if (!config.singleUseFishingRods || cir.getReturnValueI() == 0) {
            return;
        }

        FishingHook hook = (FishingHook) (Object) this;
        if (hook.getHookedIn() == null && nibble <= 0) {
            return;
        }

        Player owner = hook.getPlayerOwner();
        if (owner == null || (config.creativeExempt && owner.getAbilities().instabuild) || rod.isEmpty()) {
            return;
        }

        rod.setCount(0);
        Level level = hook.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
        }
    }
}
