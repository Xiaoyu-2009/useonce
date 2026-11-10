package dev.xiaoyu.useonce.mixin;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import dev.xiaoyu.useonce.rules.ServerTasks;
import dev.xiaoyu.useonce.rules.TradeRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
    // 交易完成后执行惩罚
    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void useonce$penaliseAfterTrade(MerchantOffer offer, CallbackInfo ci) {
        UseOnceConfig config = UseOnceConfig.get();
        UseOnceConfig.TradePenalty penalty = TradeRules.penalty(config);
        if (penalty == UseOnceConfig.TradePenalty.NONE) {
            return;
        }

        AbstractVillager self = (AbstractVillager) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Player trader = self.getTradingPlayer();
        if (config.creativeExempt && trader != null && trader.getAbilities().instabuild) {
            return;
        }

        if (penalty == UseOnceConfig.TradePenalty.DISABLE_OFFER) {
            offer.setToOutOfStock();
        } else {
            for (MerchantOffer merchantOffer : self.getOffers()) {
                merchantOffer.setToOutOfStock();
            }
        }

        if (penalty == UseOnceConfig.TradePenalty.KILL) {
            ServerTasks.defer(serverLevel, () -> {
                if (self.isAlive() && self.level() instanceof ServerLevel currentLevel) {
                    self.kill();
                }
            });
        } else if (trader instanceof ServerPlayer serverTrader && trader == self.getTradingPlayer() && trader.containerMenu instanceof MerchantMenu) {
            MerchantOffers offers = self.getOffers();
            if (!offers.isEmpty()) {
                int merchantLevel = self instanceof Villager villager ? villager.getVillagerData().getLevel() : 1;
                serverTrader.sendMerchantOffers(trader.containerMenu.containerId, offers, merchantLevel, self.getVillagerXp(), self.showProgressBar(), self.canRestock());
            }
        }
    }
}
