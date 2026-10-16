package com.sablednah.cityworldcompass.client;

import com.sablednah.cityworldcompass.Network;

import net.minecraft.client.Minecraft;

/** Client-only reactions to the server's messages. Nothing outside the client package may reference this class. */
public final class ClientHandlers {

    private ClientHandlers() {
    }

    public static void openMenu(Network.OpenMenu menu) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setScreen(new CompassScreen(menu));
    }
}
