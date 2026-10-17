package com.sablednah.cityworldcompass;

import java.util.List;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The two messages: the server's menu for the screen, and the player's pick back. */
public final class Network {

    private Network() {
    }

    /** One line of the menu: what CityWorld calls a {@code Findable}. */
    public record Entry(String id, String group, String title) {
        static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::id, ByteBufCodecs.STRING_UTF8, Entry::group,
                ByteBufCodecs.STRING_UTF8, Entry::title, Entry::new);
    }

    /** Server → client: open the screen with this menu, for the compass in this hand, now seeking {@code current}. */
    public record OpenMenu(List<Entry> entries, boolean mainHand, String current) implements CustomPacketPayload {
        public static final Type<OpenMenu> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CityWorldCompass.MODID, "open_menu"));
        static final StreamCodec<RegistryFriendlyByteBuf, OpenMenu> CODEC = StreamCodec.composite(
                Entry.CODEC.apply(ByteBufCodecs.list()), OpenMenu::entries, ByteBufCodecs.BOOL, OpenMenu::mainHand,
                ByteBufCodecs.STRING_UTF8, OpenMenu::current, OpenMenu::new);

        @Override
        public Type<OpenMenu> type() {
            return TYPE;
        }
    }

    /** Client → server: point the compass in this hand at the nearest {@code id}. */
    public record Seek(String id, boolean mainHand) implements CustomPacketPayload {
        public static final Type<Seek> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CityWorldCompass.MODID, "seek"));
        static final StreamCodec<RegistryFriendlyByteBuf, Seek> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Seek::id, ByteBufCodecs.BOOL, Seek::mainHand, Seek::new);

        @Override
        public Type<Seek> type() {
            return TYPE;
        }
    }

    static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(OpenMenu.TYPE, OpenMenu.CODEC,
                (payload, context) -> com.sablednah.cityworldcompass.client.ClientHandlers.openMenu(payload));
        registrar.playToServer(Seek.TYPE, Seek.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player)
                Search.seek(player, payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                        payload.id());
        });
    }
}
