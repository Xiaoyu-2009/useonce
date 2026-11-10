package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.rules.ContainerRules;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin {
    // 玩家破坏潜影盒时内容物作废
    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void useonce$forfeitContentsOnBreak(Level level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir) {
        if (level.getBlockEntity(pos) instanceof Container container) {
            ContainerRules.forfeitOnBreak(level, container);
        }
    }

    // 非玩家破坏潜影盒时同样作废内容物
    @Inject(method = "getDrops", at = @At("HEAD"))
    private void useonce$forfeitContentsOnDrops(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof Container container) {
            ContainerRules.forfeitOnBreak(params.getLevel(), container);
        }
    }
}
