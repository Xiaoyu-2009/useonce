package dev.xiaoyu.useonce.rules;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import dev.xiaoyu.useonce.mixin.CompoundContainerAccessor;
import dev.xiaoyu.useonce.mixin.PlayerEnderChestContainerAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public final class ContainerRules {
    private static final Set<Container> DOOMED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static Player currentBreaker;
    private static Container dispenseSource;
    private static Census dispenseBefore;
    public static boolean selfDrop;

    private ContainerRules() {
    }

    // 主动掉落容器内容（用坏机器/方块时），此掉落不作废物品
    public static void dropContentsUnforfeited(Level level, BlockPos pos, Container container) {
        selfDrop = true;
        try {
            Containers.dropContents(level, pos, container);
        } finally {
            selfDrop = false;
        }
    }

    // 判断容器是否在配置的作用范围内
    public static boolean isInScope(Container container) {
        if (container == null) {
            return false;
        }

        UseOnceConfig.ContainerScope scope = UseOnceConfig.get().containerScope;
        if (scope == null || scope == UseOnceConfig.ContainerScope.NONE) {
            return false;
        }

        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccessor halves = (CompoundContainerAccessor) compound;
            return isInScope(halves.useonce$getFirst()) || isInScope(halves.useonce$getSecond());
        }
        if (container instanceof ChestBlockEntity) {
            return true;
        }
        if (scope == UseOnceConfig.ContainerScope.CHESTS) {
            return false;
        }
        if (container instanceof BarrelBlockEntity || container instanceof ShulkerBoxBlockEntity
                || container instanceof PlayerEnderChestContainer || container instanceof MinecartChest
                || container instanceof ChestBoat) {
            return true;
        }
        if (container instanceof DispenserBlockEntity || container instanceof HopperBlockEntity || container instanceof MinecartHopper) {
            return UseOnceConfig.get().containerItemMovers || scope == UseOnceConfig.ContainerScope.ALL;
        }
        if (scope != UseOnceConfig.ContainerScope.ALL) {
            return false;
        }
        return container instanceof BaseContainerBlockEntity || container instanceof ContainerEntity;
    }

    public static boolean isDoomed(Container container) {
        if (container == null || DOOMED.isEmpty()) {
            return false;
        }
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccessor halves = (CompoundContainerAccessor) compound;
            return isDoomed(halves.useonce$getFirst()) || isDoomed(halves.useonce$getSecond());
        }
        return DOOMED.contains(container);
    }

    public static boolean menuTouchesDoomedContainer(AbstractContainerMenu menu) {
        if (DOOMED.isEmpty()) {
            return false;
        }
        for (Slot slot : menu.slots) {
            if (isInScope(slot.container) && isDoomed(slot.container)) {
                return true;
            }
        }
        return false;
    }

    private static void markDoomed(Container container) {
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccessor halves = (CompoundContainerAccessor) compound;
            markDoomed(halves.useonce$getFirst());
            markDoomed(halves.useonce$getSecond());
        } else {
            DOOMED.add(container);
        }
    }

    public static void beginPlayerBreak(Player player) {
        currentBreaker = player;
    }

    public static void endPlayerBreak() {
        currentBreaker = null;
    }

    private static Census census(Container container) {
        Census census = new Census();
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            census.add(container.getItem(i));
        }
        return census;
    }

    public static Census snapshotContents(Container container) {
        return census(container);
    }

    public static boolean wasTakenFrom(Census before, Container container) {
        return before.lostGroundTo(census(container));
    }

    // 记录菜单中所有作用范围内的容器内容
    public static MenuSnapshot snapshot(AbstractContainerMenu menu) {
        List<Container> found = null;
        for (Slot slot : menu.slots) {
            Container container = slot.container;
            if (!isInScope(container)) {
                continue;
            }
            if (found == null) {
                found = new ArrayList<>(1);
            } else if (containsIdentical(found, container)) {
                continue;
            }
            found.add(container);
        }
        return found == null ? null : new MenuSnapshot(found);
    }

    private static boolean containsIdentical(List<Container> containers, Container container) {
        for (Container existing : containers) {
            if (existing == container) {
                return true;
            }
        }
        return false;
    }

    public static void beginDispense(Container container) {
        dispenseSource = null;
        dispenseBefore = null;
        if (UseOnceConfig.get().containerPushExtraction && isInScope(container) && !isDoomed(container)) {
            dispenseSource = container;
            dispenseBefore = census(container);
        }
    }

    public static void endDispense() {
        Container container = dispenseSource;
        Census before = dispenseBefore;
        dispenseSource = null;
        dispenseBefore = null;
        if (container != null && before.lostGroundTo(census(container))) {
            breakContainer(container);
        }
    }

    // 破坏被抽空的容器
    public static void breakContainer(Container container) {
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccessor halves = (CompoundContainerAccessor) compound;
            breakContainer(halves.useonce$getFirst());
            breakContainer(halves.useonce$getSecond());
            return;
        }

        if (container instanceof PlayerEnderChestContainer enderChest) {
            breakEnderChest(enderChest);
            return;
        }

        if (container instanceof BlockEntity blockEntity) {
            Level level = blockEntity.getLevel();
            if (level instanceof ServerLevel serverLevel) {
                BlockPos pos = blockEntity.getBlockPos();
                markDoomed(container);
                ServerTasks.defer(serverLevel, () -> {
                    try {
                        if (serverLevel.getBlockEntity(pos) == blockEntity) {
                            closeViewers(serverLevel, container);
                            emptyOut(serverLevel, container, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                            playBreak(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundSource.BLOCKS);
                            serverLevel.destroyBlock(pos, false);
                        }
                    } finally {
                        DOOMED.remove(container);
                    }
                });
            }
            return;
        }

        if (container instanceof Entity entity) {
            if (!(entity.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            markDoomed(container);
            ServerTasks.defer(serverLevel, () -> {
                try {
                    if (!entity.isRemoved()) {
                        closeViewers(serverLevel, container);
                        emptyOut(serverLevel, container, entity.getX(), entity.getY(), entity.getZ());
                        playBreak(serverLevel, entity.getX(), entity.getY(), entity.getZ(), SoundSource.PLAYERS);
                        entity.discard();
                    }
                } finally {
                    DOOMED.remove(container);
                }
            });
        }
    }

    private static void breakEnderChest(PlayerEnderChestContainer container) {
        EnderChestBlockEntity chest = ((PlayerEnderChestContainerAccessor) container).useonce$getActiveChest();
        if (chest != null && chest.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos pos = chest.getBlockPos();
            markDoomed(container);
            ServerTasks.defer(serverLevel, () -> {
                try {
                    closeViewers(serverLevel, container);
                    emptyOut(serverLevel, container, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (serverLevel.getBlockEntity(pos) == chest) {
                        playBreak(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundSource.BLOCKS);
                        serverLevel.destroyBlock(pos, false);
                    }
                } finally {
                    DOOMED.remove(container);
                }
            });
            return;
        }
        container.clearContent();
    }

    // 玩家破坏容器时内容物作废
    public static void forfeitOnBreak(Level level, Container container) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        UseOnceConfig config = UseOnceConfig.get();
        if (!config.containerBreakForfeit || !config.containerContentsVanish || !isInScope(container)) {
            return;
        }
        Player breaker = currentBreaker;
        if (breaker != null && breaker.getAbilities().instabuild && config.creativeExempt) {
            return;
        }
        if (container instanceof RandomizableContainer randomizable) {
            randomizable.unpackLootTable(null);
        }
        container.clearContent();
    }

    private static void emptyOut(ServerLevel level, Container container, double x, double y, double z) {
        if (container instanceof RandomizableContainer randomizable) {
            randomizable.unpackLootTable(null);
        }
        if (!UseOnceConfig.get().containerContentsVanish) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                Containers.dropItemStack(level, x, y, z, container.getItem(i));
            }
        }
        container.clearContent();
    }

    // 关闭正在查看该容器的玩家界面
    private static void closeViewers(ServerLevel level, Container container) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            AbstractContainerMenu menu = player.containerMenu;
            if (menu != player.inventoryMenu && menuShows(menu, container)) {
                player.closeContainer();
            }
        }
    }

    private static boolean menuShows(AbstractContainerMenu menu, Container container) {
        for (Slot slot : menu.slots) {
            Container slotContainer = slot.container;
            if (slotContainer == container) {
                return true;
            }
            if (slotContainer instanceof CompoundContainer compound && compound.contains(container)) {
                return true;
            }
        }
        return false;
    }

    private static void playBreak(ServerLevel level, double x, double y, double z, SoundSource source) {
        level.playSound(null, x, y, z, SoundEvents.ITEM_BREAK, source, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
    }

    // 容器内容物统计（用于比较物品是否被取出）
    public static final class Census {
        private ItemStack[] kinds = new ItemStack[8];
        private int[] totals = new int[8];
        private int size;

        private void add(ItemStack stack) {
            if (stack.isEmpty()) {
                return;
            }
            BundleContents nested = stack.get(DataComponents.BUNDLE_CONTENTS);
            ItemStack key = stack.copy();
            if (nested != null) {
                key.set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            }
            addKind(key, stack.getCount());
            if (nested != null) {
                nested.itemCopyStream().forEach(this::add);
            }
        }

        private void addKind(ItemStack key, int count) {
            for (int i = 0; i < size; i++) {
                if (ItemStack.isSameItemSameComponents(kinds[i], key)) {
                    totals[i] += count;
                    return;
                }
            }
            if (size == kinds.length) {
                kinds = Arrays.copyOf(kinds, size * 2);
                totals = Arrays.copyOf(totals, size * 2);
            }
            kinds[size] = key;
            totals[size] = count;
            size++;
        }

        private int total(ItemStack key) {
            for (int i = 0; i < size; i++) {
                if (ItemStack.isSameItemSameComponents(kinds[i], key)) {
                    return totals[i];
                }
            }
            return 0;
        }

        private boolean lostGroundTo(Census later) {
            for (int i = 0; i < size; i++) {
                if (later.total(kinds[i]) < totals[i]) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class MenuSnapshot {
        private final Container[] containers;
        private final Census[] before;

        private MenuSnapshot(List<Container> found) {
            containers = found.toArray(new Container[0]);
            before = new Census[containers.length];
            for (int i = 0; i < containers.length; i++) {
                before[i] = census(containers[i]);
            }
        }

        public void breakDrainedContainers() {
            for (int i = 0; i < containers.length; i++) {
                if (wasTakenFrom(before[i], containers[i])) {
                    breakContainer(containers[i]);
                }
            }
        }
    }
}
