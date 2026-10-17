package com.sablednah.cityworldcompass;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** The two messages: the server's menu for the screen, and the player's pick back (Forge 1.20.1: a SimpleChannel). */
public final class Network {

    private Network() {
    }

    private static final String VERSION = "1";
    static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CityWorldCompass.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    /** One line of the menu: what CityWorld calls a {@code Findable}. */
    public record Entry(String id, String group, String title) {
    }

    /** Server → client: open the screen with this menu, for the compass in this hand, now seeking {@code current}. */
    public record OpenMenu(List<Entry> entries, boolean mainHand, String current) {
        void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(entries.size());
            for (Entry e : entries) {
                buf.writeUtf(e.id());
                buf.writeUtf(e.group());
                buf.writeUtf(e.title());
            }
            buf.writeBoolean(mainHand);
            buf.writeUtf(current);
        }

        static OpenMenu decode(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<Entry> entries = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
                entries.add(new Entry(buf.readUtf(), buf.readUtf(), buf.readUtf()));
            return new OpenMenu(entries, buf.readBoolean(), buf.readUtf());
        }

        public void handle(Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.sablednah.cityworldcompass.client.ClientHandlers.openMenu(this)));
            context.get().setPacketHandled(true);
        }
    }

    /** Client → server: point the compass in this hand at the nearest {@code id}. */
    public record Seek(String id, boolean mainHand) {
        void encode(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeBoolean(mainHand);
        }

        static Seek decode(FriendlyByteBuf buf) {
            return new Seek(buf.readUtf(), buf.readBoolean());
        }

        public void handle(Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player != null)
                    Search.seek(player, mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, id);
            });
            context.get().setPacketHandled(true);
        }
    }

    static void register() {
        CHANNEL.messageBuilder(OpenMenu.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenMenu::encode).decoder(OpenMenu::decode).consumerNetworkThread(OpenMenu::handle).add();
        CHANNEL.messageBuilder(Seek.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Seek::encode).decoder(Seek::decode).consumerNetworkThread(Seek::handle).add();
    }

    static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
