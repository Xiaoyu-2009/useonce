package dev.xiaoyu.useonce.mixin;

import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.StonecutterMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StonecutterMenu.class)
public interface StonecutterMenuAccessor {
    @Accessor("access")
    ContainerLevelAccess useonce$getAccess();
}
