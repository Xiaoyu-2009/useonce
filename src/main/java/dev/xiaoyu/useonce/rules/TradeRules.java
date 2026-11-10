package dev.xiaoyu.useonce.rules;

import dev.xiaoyu.useonce.config.UseOnceConfig;

public final class TradeRules {
    private TradeRules() {
    }

    public static UseOnceConfig.TradePenalty penalty(UseOnceConfig config) {
        UseOnceConfig.TradePenalty penalty = config.villagerTradePenalty;
        return penalty == null ? UseOnceConfig.TradePenalty.NONE : penalty;
    }

    public static boolean blocksRestock() {
        UseOnceConfig.TradePenalty penalty = penalty(UseOnceConfig.get());
        return penalty == UseOnceConfig.TradePenalty.DISABLE_OFFER || penalty == UseOnceConfig.TradePenalty.DISABLE_ALL;
    }
}
