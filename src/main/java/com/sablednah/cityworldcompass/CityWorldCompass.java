package com.sablednah.cityworldcompass;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import org.slf4j.Logger;

/**
 * CityWorld Compass: Nature's Compass for CityWorld's cities. Right-click, pick a kind of place, and the needle
 * points to the nearest one. The search is CityWorld's ({@code CityWorldAPI.findables} / {@code find}): it reads the
 * city plan, so nothing is generated to answer it.
 */
@Mod(CityWorldCompass.MODID)
public final class CityWorldCompass {

    public static final String MODID = "cityworld_compass";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<UrbanCompassItem> URBAN_COMPASS = ITEMS.registerItem("urban_compass",
            UrbanCompassItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister
            .create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);
    static {
        LOOT_MODIFIERS.register("cityworld_chests", () -> CityWorldChestLoot.CODEC);
    }

    public CityWorldCompass(IEventBus modBus, ModContainer container) {
        ITEMS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        modBus.addListener(Network::register);
        modBus.addListener(CityWorldCompass::creativeTab);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
            modBus.addListener(com.sablednah.cityworldcompass.client.ClientHandlers::setup);
        container.registerConfig(ModConfig.Type.STARTUP, Config.SPEC);
        LOGGER.info("CityWorld Compass {} initialising", container.getModInfo().getVersion());
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
            event.accept(URBAN_COMPASS);
    }
}
