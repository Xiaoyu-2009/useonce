package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.rules.ContainerRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DispenserBlock.class, DropperBlock.class})
public abstract class DispenseFromMixin {
    // 发射/投掷器工作时检测内容物是否被取出
    @Inject(method = "dispenseFrom", at = @At("HEAD"))
    private void useonce$beginDispense(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (level.getBlockEntity(pos) instanceof Container container) {
            ContainerRules.beginDispense(container);
        }
    }

    @Inject(method = "dispenseFrom", at = @At("RETURN"))
    private void useonce$endDispense(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
        ContainerRules.endDispense();
    }
}
