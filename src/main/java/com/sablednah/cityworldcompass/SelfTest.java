package com.sablednah.cityworldcompass;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Dev check, run only with {@code -Dcityworld_compass.selftest=true} (gradle {@code -Pselftest}): the menu, a few
 * searches through CityWorld's API, the recipe, and the chest loot rate. It logs {@code COMPASS SELFTEST} lines and
 * never stops the server: the script that started it does that.
 */
@EventBusSubscriber(modid = CityWorldCompass.MODID)
public final class SelfTest {

    private SelfTest() {
    }

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean("cityworld_compass.selftest"))
            return;
        MinecraftServer server = event.getServer();
        new Thread(() -> run(server), "cityworld-compass-selftest").start();
    }

    private static void run(MinecraftServer server) {
        List<String> failures = new ArrayList<>();
        ServerLevel level = server.overworld();
        try {
            var menu = Search.menu(level);
            log("menu entries " + menu.size());
            if (menu.size() < 50)
                failures.add("menu has " + menu.size() + " entries");
            for (String id : List.of("place:subway", "city", "district:industrial", "shop:bookshop", "building:winchester",
                    "place:hospital")) {
                long t0 = System.nanoTime();
                var at = Search.locate(level, BlockPos.ZERO, id, 3000, 60_000);
                log(id + " -> " + at.map(BlockPos::toShortString).orElse("none") + " in "
                        + (System.nanoTime() - t0) / 1_000_000 + " ms");
                if (at.isEmpty() && (id.equals("place:subway") || id.equals("city")))
                    failures.add(id + " found nothing");
            }
            var recipe = server.getRecipeManager().byKey(
                    ResourceLocation.fromNamespaceAndPath(CityWorldCompass.MODID, "urban_compass"));
            log("recipe " + (recipe.isPresent() ? "loaded" : "MISSING"));
            if (recipe.isEmpty())
                failures.add("recipe missing");
            int rolls = 4000, compasses = 0, extraHits = 0;
            var table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath("cityworld", "chests/building")));
            var extra = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath("cityworld", "chests/building_extra")));
            for (int i = 0; i < rolls; i++) {
                LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                        .create(LootContextParamSets.CHEST);
                for (ItemStack s : table.getRandomItems(params))
                    if (s.is(CityWorldCompass.URBAN_COMPASS.get()))
                        compasses++;
                for (ItemStack s : extra.getRandomItems(params))
                    if (s.is(CityWorldCompass.URBAN_COMPASS.get()))
                        extraHits++;
            }
            log("chests/building: " + compasses + " compasses in " + rolls + " rolls; chests/building_extra: " + extraHits);
            if (compasses == 0 || compasses > rolls / 40)
                failures.add("loot rate " + compasses + "/" + rolls);
            if (extraHits != 0)
                failures.add("an _extra table gave a compass");
        } catch (Throwable t) {
            failures.add("threw " + t);
            CityWorldCompass.LOGGER.error("COMPASS SELFTEST threw", t);
        }
        log(failures.isEmpty() ? "PASS" : "FAIL " + failures);
        log("complete");
    }

    private static void log(String line) {
        CityWorldCompass.LOGGER.warn("COMPASS SELFTEST: {}", line);
    }
}
