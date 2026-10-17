package com.sablednah.cityworldcompass.client;

import com.sablednah.cityworldcompass.Network;

import net.minecraft.client.Minecraft;

/** Client-only reactions to the server's messages. Nothing outside the client package may reference this class. */
public final class ClientHandlers {

    private ClientHandlers() {
    }

    public static void openMenu(Network.OpenMenu menu) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new CompassScreen(menu));
    }

    /** The needle: vanilla's compass angle, aimed at the stack's lodestone target (none: it spins). */
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(
                com.sablednah.cityworldcompass.CityWorldCompass.URBAN_COMPASS.get(),
                new net.minecraft.resources.ResourceLocation("angle"),
                new net.minecraft.client.renderer.item.CompassItemPropertyFunction((level, stack, entity) ->
                        net.minecraft.world.item.CompassItem.isLodestoneCompass(stack)
                                ? net.minecraft.world.item.CompassItem.getLodestonePosition(stack.getOrCreateTag())
                                : null)));
    }
}
