package com.sablednah.cityworldcompass;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.slf4j.Logger;

/**
 * CityWorld Compass: Nature's Compass for CityWorld's cities. Right-click, pick a kind of place, and the needle
 * points to the nearest one. The search is CityWorld's ({@code CityWorldAPI.findables} / {@code find}): it reads the
 * city plan, so nothing is generated to answer it. (Forge 1.20.1: FML injects nothing, so the bus comes from
 * {@code FMLJavaModLoadingContext}.)
 */
@Mod(CityWorldCompass.MODID)
public final class CityWorldCompass {

    public static final String MODID = "cityworld_compass";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> URBAN_COMPASS = ITEMS.register("urban_compass",
            () -> new UrbanCompassItem(new Item.Properties().stacksTo(1)));

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister
            .create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);
    static {
        LOOT_MODIFIERS.register("cityworld_chests", CityWorldChestLoot.CODEC);
    }

    public CityWorldCompass() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        modBus.addListener(CityWorldCompass::creativeTab);
        if (FMLEnvironment.dist.isClient())
            modBus.addListener(com.sablednah.cityworldcompass.client.ClientHandlers::setup);
        Network.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("CityWorld Compass {} initialising", ModLoadingContext.get().getActiveContainer().getModInfo().getVersion());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) ->
                        Search.forget(e.getEntity().getUUID()));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.server.ServerStoppedEvent e) -> Search.forget(null));
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
            event.accept(URBAN_COMPASS);
    }
}
