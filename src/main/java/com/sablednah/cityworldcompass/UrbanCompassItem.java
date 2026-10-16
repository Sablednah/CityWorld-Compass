package com.sablednah.cityworldcompass;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The compass. Use: open the menu. Sneak-use: search again for what it already seeks (from where you stand now).
 * The needle is vanilla's lodestone needle, so the item model only has to dispatch on it.
 */
public final class UrbanCompassItem extends Item {

    public UrbanCompassItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            ItemStack stack = player.getItemInHand(hand);
            String current = Search.target(stack);
            if (player.isShiftKeyDown() && !current.isEmpty())
                Search.seek(server, hand, current);
            else {
                var menu = Search.menu(server.level());
                if (menu.isEmpty())
                    server.sendOverlayMessage(Component.translatable("cityworld_compass.not_cityworld"));
                else
                    PacketDistributor.sendToPlayer(server,
                            new Network.OpenMenu(menu, hand == InteractionHand.MAIN_HAND, current));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        String title = Search.title(stack);
        if (!title.isEmpty())
            builder.accept(Component.translatable("cityworld_compass.seeking", title).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("cityworld_compass.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
