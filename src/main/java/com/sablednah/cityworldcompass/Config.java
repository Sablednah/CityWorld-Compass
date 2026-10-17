package com.sablednah.cityworldcompass;

import net.minecraftforge.common.ForgeConfigSpec;

/** How far and how long a search may go (config/cityworld_compass-common.toml). */
public final class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue RADIUS = BUILDER
            .comment("How far a search reaches, in blocks.")
            .defineInRange("searchRadius", 3000, 160, 20000);

    public static final ForgeConfigSpec.IntValue BUDGET_SECONDS = BUILDER
            .comment("The longest a search may run before it gives up, in seconds. It runs off the server thread, and a second try\n"
                    + "goes further: the part of the city plan already worked out is kept.")
            .defineInRange("searchSeconds", 60, 1, 300);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
