package com.sablednah.cityworldcompass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * A compass, now and then, in a CityWorld chest: any {@code cityworld:chests/...} table the game rolls, with
 * {@code chance} per roll. Matching CityWorld's namespace rather than listing tables keeps up with new ones, and it
 * leaves CityWorld's {@code _extra} hook tables alone (a pack owns those).
 */
public final class CityWorldChestLoot extends LootModifier {

    public static final MapCodec<CityWorldChestLoot> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i)
            .and(Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(m -> m.chance))
            .apply(i, CityWorldChestLoot::new));

    private final float chance;

    public CityWorldChestLoot(java.util.Optional<net.minecraft.core.Holder<LootItemCondition>> condition, int priority, float chance) {
        super(condition, priority);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var id = context.getQueriedLootTableId();
        if (id != null && id.getNamespace().equals("cityworld") && id.getPath().startsWith("chests/")
                && !id.getPath().endsWith("_extra") && context.getRandom().nextFloat() < chance)
            loot.add(new ItemStack(CityWorldCompass.URBAN_COMPASS.get()));
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
