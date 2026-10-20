package com.sablednah.cityworldcompass;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.daddychurchill.CityWorld.api.CityWorldAPI;
import me.daddychurchill.CityWorld.api.Findable;
import me.daddychurchill.CityWorld.api.LotInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * The server side of a compass: the menu, and a search off the server thread that points the needle when it
 * answers. One search per player at a time.
 */
public final class Search {

    private Search() {
    }

    /** Custom-data keys on the compass: what it seeks, and its title. */
    static final String TARGET = "cityworld_compass_target", TITLE = "cityworld_compass_title";

    private static final Set<UUID> SEARCHING = ConcurrentHashMap.newKeySet();
    /** Keeps "seeking..." on screen while a long search runs (an overlay message fades after a few seconds). */
    private static final java.util.concurrent.ScheduledExecutorService TICKER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cityworld-compass-ticker");
        t.setDaemon(true);
        return t;
    });

    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "cityworld-compass-search");
        t.setDaemon(true);
        return t;
    });

    /** What this level has to find, as menu lines; empty off a CityWorld level. */
    static List<Network.Entry> menu(ServerLevel level) {
        return CityWorldAPI.findables(level).stream().map(f -> new Network.Entry(f.id(), f.group(), f.title())).toList();
    }

    /** The id the compass seeks, or "" for none. */
    static String target(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TARGET);
    }

    static String title(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TITLE);
    }

    /** Point the needle: vanilla's lodestone tags (1.20.1 has no data components), untracked so no lodestone is needed. */
    private static void point(ItemStack stack, Level level, BlockPos at) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(CompassItem.TAG_LODESTONE_POS, NbtUtils.writeBlockPos(at));
        Level.RESOURCE_KEY_CODEC.encodeStart(NbtOps.INSTANCE, level.dimension()).result()
                .ifPresent(dim -> tag.put(CompassItem.TAG_LODESTONE_DIMENSION, dim));
        tag.putBoolean(CompassItem.TAG_LODESTONE_TRACKED, false);
    }

    private static void unpoint(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(CompassItem.TAG_LODESTONE_POS);
            tag.remove(CompassItem.TAG_LODESTONE_DIMENSION);
            tag.remove(CompassItem.TAG_LODESTONE_TRACKED);
        }
    }

    /** Search for the nearest {@code id} from where the player stands, and point the compass in {@code hand} at it. */
    public static void seek(ServerPlayer player, InteractionHand hand, String id) {
        if (!player.getItemInHand(hand).is(CityWorldCompass.URBAN_COMPASS.get()))
            return;
        ServerLevel level = player.serverLevel();
        Optional<Findable> wanted = CityWorldAPI.findables(level).stream().filter(f -> f.id().equals(id)).findFirst();
        if (wanted.isEmpty()) {
            player.displayClientMessage(Component.translatable("cityworld_compass.unknown"), true);
            return;
        }
        if (!SEARCHING.add(player.getUUID())) {
            player.displayClientMessage(Component.translatable("cityworld_compass.busy"), true);
            return;
        }
        String title = wanted.get().title();
        BlockPos from = player.blockPosition();
        int radius = Config.RADIUS.get();
        long budget = Config.BUDGET_SECONDS.get() * 1000L;
        player.displayClientMessage(Component.translatable("cityworld_compass.searching", title), true);
        long started = System.nanoTime();
        var ticker = TICKER.scheduleAtFixedRate(() -> level.getServer().execute(() -> {
            if (SEARCHING.contains(player.getUUID()))
                player.displayClientMessage(Component.translatable("cityworld_compass.still_searching", title,
                        (System.nanoTime() - started) / 1_000_000_000L), true);
        }), 2, 2, java.util.concurrent.TimeUnit.SECONDS);
        WORKERS.execute(() -> {
            Optional<BlockPos> found = Optional.empty();
            long t0 = System.nanoTime();
            try {
                found = locate(level, from, id, radius, budget);
            } catch (RuntimeException e) {
                CityWorldCompass.LOGGER.warn("CityWorld Compass: search for {} failed", id, e);
            }
            long millis = (System.nanoTime() - t0) / 1_000_000;
            // CityWorld's find stops at the radius or the budget, and answers empty either way: a search that ran
            // its whole budget may simply not have got far enough yet (the plan it worked out is kept, so a second
            // try goes further)
            boolean gaveUp = found.isEmpty() && millis >= budget * 95 / 100;
            CityWorldCompass.LOGGER.info("CityWorld Compass: {} sought {} from {}: {} in {} ms", player.getName().getString(),
                    id, from.toShortString(), found.map(BlockPos::toShortString).orElse(gaveUp ? "gave up" : "none"), millis);
            Optional<BlockPos> result = found;
            ticker.cancel(false);
            // Released here, not on the server thread: a world closed mid-search never runs that task, and in single
            // player the same UUID would then be "busy" in every world until the game restarted.
            SEARCHING.remove(player.getUUID());
            level.getServer().execute(() -> {
                ItemStack stack = player.getItemInHand(hand);
                if (!stack.is(CityWorldCompass.URBAN_COMPASS.get()))
                    return; // put away while it searched
                CompoundTag tag = stack.getOrCreateTag();
                tag.putString(TARGET, id);
                tag.putString(TITLE, title);
                if (result.isPresent()) {
                    BlockPos at = result.get();
                    point(stack, level, at);
                    int distance = (int) Math.round(Math.hypot(at.getX() - player.getX(), at.getZ() - player.getZ()));
                    player.displayClientMessage(Component.translatable("cityworld_compass.found", title, distance), true);
                } else {
                    unpoint(stack); // the needle spins
                    player.displayClientMessage(gaveUp
                            ? Component.translatable("cityworld_compass.gave_up", title, budget / 1000)
                            : Component.translatable("cityworld_compass.none", title, radius), true);
                }
            });
        });
    }

    /** Forget who is searching: one player when they log out, or everyone (null) when the server stops. */
    static void forget(@org.jetbrains.annotations.Nullable java.util.UUID player) {
        if (player == null)
            SEARCHING.clear();
        else
            SEARCHING.remove(player);
    }

    /** Where to point for the nearest {@code id}: a vault's door, else the middle of the chunk at street level. */
    static Optional<BlockPos> locate(ServerLevel level, BlockPos from, String id, int radius, long budgetMillis) {
        List<LotInfo> hits = CityWorldAPI.find(level, from, id, radius, 1, budgetMillis);
        if (hits.isEmpty())
            return Optional.empty();
        LotInfo lot = hits.get(0);
        if (id.equals("place:vault")) {
            Optional<BlockPos> door = CityWorldAPI.vaultEntrance(level, lot.chunk());
            if (door.isPresent())
                return door;
        }
        return Optional.of(new BlockPos(lot.chunk().getMiddleBlockX(), lot.streetLevel() + 1, lot.chunk().getMiddleBlockZ()));
    }
}
