package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import dev.xiaoyu.useonce.duck.DoomedMenu;
import dev.xiaoyu.useonce.rules.ServerTasks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public abstract class StonecutterResultSlotMixin {
    // 切石机使用一次后损坏
    @Inject(method = "onTake", at = @At("RETURN"))
    private void useonce$breakStonecutter(Player player, ItemStack stack, CallbackInfo ci) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof StonecutterMenu stonecutterMenu) || stonecutterMenu.getSlot(1) != (Object) this) {
            return;
        }
        UseOnceConfig config = UseOnceConfig.get();
        if (!config.singleUseStonecutters || (config.creativeExempt && player.getAbilities().instabuild)) {
            return;
        }
        if (!config.stonecutterBatch) {
            stonecutterMenu.getSlot(1).set(ItemStack.EMPTY);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        DoomedMenu doomed = (DoomedMenu) stonecutterMenu;
        if (doomed.useonce$isMenuDoomed()) {
            return;
        }
        doomed.useonce$doomMenu();
        ((StonecutterMenuAccessor) stonecutterMenu).useonce$getAccess().execute((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                BlockPos target = pos.immutable();
                ServerTasks.defer(serverLevel, () -> {
                    if (serverPlayer.containerMenu == stonecutterMenu) {
                        if (serverLevel.getBlockState(target).is(Blocks.STONECUTTER)) {
                            serverLevel.playSound(null, target, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
                            serverLevel.destroyBlock(target, false);
                        }
                        serverPlayer.closeContainer();
                    }
                });
            }
        });
    }
}
