package dev.xiaoyu.useonce;

import dev.xiaoyu.useonce.config.UseOnceConfig;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(UseOnce.MOD_ID)
public class UseOnce {
    public static final String MOD_ID = "useonce";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public UseOnce() {
        UseOnceConfig config = UseOnceConfig.get();
        LOGGER.info(
                "[Use Once] 已加载 — 物品={} (鱼竿豁免={}) 鱼竿={} 桶={} 船={} 熔炉={} 酿造台={} 合成器={} 工作台={} (批量={}) 切石机={} (批量={}) 床={} 门={} (红石={}) 容器={} (物流方块={} 内容消失={} 漏斗抽取={} 推出={} 生物抽取={} 破坏作废={} 一键拾取={}) 村民={} 以物易物={} 下界传送门={} 末地传送门={} (仅玩家={}) 创造豁免={}",
                config.singleUseItems,
                config.fishingRodExempt,
                config.singleUseFishingRods,
                config.singleUseBuckets,
                config.singleUseBoats,
                config.singleUseFurnaces,
                config.singleUseBrewingStands,
                config.singleUseCrafters,
                config.singleUseCraftingTables,
                config.craftingTableBatch,
                config.singleUseStonecutters,
                config.stonecutterBatch,
                config.singleUseBeds,
                config.singleUseDoors,
                config.doorsBreakOnRedstone,
                config.containerScope == null ? UseOnceConfig.ContainerScope.NONE : config.containerScope,
                config.containerItemMovers,
                config.containerContentsVanish,
                config.containerHopperExtraction,
                config.containerPushExtraction,
                config.containerMobExtraction,
                config.containerBreakForfeit,
                config.containerBlockPickAll,
                config.villagerTradePenalty == null ? UseOnceConfig.TradePenalty.NONE : config.villagerTradePenalty,
                config.piglinBarterOnce,
                config.netherPortalSingleUse,
                config.endPortalSingleUse,
                config.portalsPlayersOnly,
                config.creativeExempt
        );
    }
}