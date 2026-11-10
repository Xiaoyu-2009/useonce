package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import dev.xiaoyu.useonce.duck.DoomedMenu;
import dev.xiaoyu.useonce.rules.ContainerRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin implements DoomedMenu {
    @Unique
    private ContainerRules.MenuSnapshot useonce$snapshot;
    @Unique
    private boolean useonce$doomed;
    @Unique
    private Player useonce$clicker;

    // 点击前记录容器内容，若菜单已作废则拦截操作
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void useonce$snapshotContainers(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        useonce$clicker = player;
        useonce$snapshot = null;
        if (player instanceof ServerPlayer) {
            if (!player.getAbilities().instabuild || !UseOnceConfig.get().creativeExempt) {
                if (useonce$doomed) {
                    useonce$clicker = null;
                    ci.cancel();
                } else if (ContainerRules.menuTouchesDoomedContainer((AbstractContainerMenu) (Object) this)) {
                    useonce$clicker = null;
                    ci.cancel();
                } else {
                    useonce$snapshot = ContainerRules.snapshot((AbstractContainerMenu) (Object) this);
                }
            }
        }
    }

    // 点击后破坏被取出物品的容器
    @Inject(method = "clicked", at = @At("RETURN"))
    private void useonce$breakDrainedContainers(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        ContainerRules.MenuSnapshot snapshot = useonce$snapshot;
        useonce$snapshot = null;
        useonce$clicker = null;
        if (snapshot != null) {
            snapshot.breakDrainedContainers();
        }
    }

    @Override
    public boolean useonce$isMenuDoomed() {
        return useonce$doomed;
    }

    @Override
    public void useonce$doomMenu() {
        useonce$doomed = true;
    }

    // 禁止一键拾取容器内物品
    @Inject(method = "canTakeItemForPickAll", at = @At("HEAD"), cancellable = true)
    private void useonce$blockPickAll(ItemStack carried, Slot target, CallbackInfoReturnable<Boolean> cir) {
        if (!UseOnceConfig.get().containerBlockPickAll) {
            return;
        }
        Player clicker = useonce$clicker;
        if (clicker == null || !clicker.getAbilities().instabuild || !UseOnceConfig.get().creativeExempt) {
            if (ContainerRules.isInScope(target.container)) {
                cir.setReturnValue(false);
            }
        }
    }
}
