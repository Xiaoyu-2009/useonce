package dev.xiaoyu.useonce.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import dev.xiaoyu.useonce.UseOnce;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

public final class UseOnceConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final String FILE_NAME = UseOnce.MOD_ID + ".json";
    private static volatile UseOnceConfig instance;

    public boolean singleUseItems = true;
    public boolean fishingRodExempt = true;
    public boolean singleUseFishingRods = true;
    public boolean singleUseBuckets = true;
    public boolean singleUseBoats = true;
    public boolean singleUseFurnaces = true;
    public boolean singleUseBrewingStands = true;
    public boolean singleUseCrafters = true;
    public boolean singleUseCraftingTables = true;
    public boolean craftingTableBatch = true;
    public boolean singleUseStonecutters = true;
    public boolean stonecutterBatch = true;
    public boolean singleUseBeds = true;
    public boolean singleUseDoors = true;
    public boolean doorsBreakOnRedstone = false;
    public ContainerScope containerScope = ContainerScope.STORAGE;
    public boolean containerItemMovers = true;
    public boolean containerContentsVanish = true;
    public boolean containerHopperExtraction = true;
    public boolean containerPushExtraction = true;
    public boolean containerMobExtraction = true;
    public boolean containerBreakForfeit = true;
    public boolean containerBlockPickAll = true;
    public TradePenalty villagerTradePenalty = TradePenalty.KILL;
    public boolean piglinBarterOnce = true;
    public boolean netherPortalSingleUse = true;
    public boolean endPortalSingleUse = true;
    public boolean portalsPlayersOnly = true;
    public boolean creativeExempt = true;

    public static UseOnceConfig get() {
        UseOnceConfig config = instance;
        if (config == null) {
            synchronized (UseOnceConfig.class) {
                config = instance;
                if (config == null) {
                    config = load();
                    instance = config;
                }
            }
        }
        return config;
    }

    public static void reload() {
        instance = load();
    }

    private static UseOnceConfig load() {
        Path path;
        try {
            path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        } catch (Throwable throwable) {
            return new UseOnceConfig();
        }

        if (!Files.exists(path)) {
            UseOnceConfig defaults = new UseOnceConfig();
            defaults.save(path);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            UseOnceConfig parsed = GSON.fromJson(reader, UseOnceConfig.class);
            if (parsed != null) {
                parsed.save(path);
                return parsed;
            }
            UseOnce.LOGGER.warn("[Use Once] {} 内容为空，使用默认配置。", path);
        } catch (JsonSyntaxException | IOException exception) {
            UseOnce.LOGGER.warn("[Use Once] 无法读取 {} ({})，使用默认配置。", path, exception.toString());
        }
        return new UseOnceConfig();
    }

    private void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            UseOnce.LOGGER.warn("[Use Once] 无法写入 {} ({})。", path, exception.toString());
        }
    }

    // 容器作用范围
    public enum ContainerScope {
        NONE,
        CHESTS,
        STORAGE,
        ALL
    }

    // 村民交易惩罚
    public enum TradePenalty {
        NONE,
        DISABLE_OFFER,
        DISABLE_ALL,
        KILL
    }
}