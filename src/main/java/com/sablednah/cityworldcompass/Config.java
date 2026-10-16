package com.sablednah.cityworldcompass;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Startup config: how far and how long a search may go (config/cityworld_compass-startup.toml). */
public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue RADIUS = BUILDER
            .comment("How far a search reaches, in blocks.")
            .defineInRange("searchRadius", 3000, 160, 20000);

    public static final ModConfigSpec.IntValue BUDGET_SECONDS = BUILDER
            .comment("The longest a search may run before it gives up, in seconds. It runs off the server thread, and a second try\n"
                    + "goes further: the part of the city plan already worked out is kept.")
            .defineInRange("searchSeconds", 60, 1, 300);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
