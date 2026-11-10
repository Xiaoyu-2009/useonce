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
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public class ResultSlotMixin {
    // 工作台合成一次后损坏
    @Inject(method = "onTake", at = @At("TAIL"))
    private void useonce$breakCraftingTable(Player player, ItemStack stack, CallbackInfo ci) {
        UseOnceConfig config = UseOnceConfig.get();
        if (!config.singleUseCraftingTables || (config.creativeExempt && player.getAbilities().instabuild)) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof CraftingMenu craftingMenu)) {
            return;
        }
        if (!config.craftingTableBatch) {
            craftingMenu.getSlot(0).set(ItemStack.EMPTY);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        DoomedMenu doomed = (DoomedMenu) craftingMenu;
        if (doomed.useonce$isMenuDoomed()) {
            return;
        }
        doomed.useonce$doomMenu();
        ((CraftingMenuAccessor) craftingMenu).useonce$getAccess().execute((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                BlockPos target = pos.immutable();
                ServerTasks.defer(serverLevel, () -> {
                    if (serverPlayer.containerMenu == craftingMenu) {
                        if (serverLevel.getBlockState(target).is(Blocks.CRAFTING_TABLE)) {
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
