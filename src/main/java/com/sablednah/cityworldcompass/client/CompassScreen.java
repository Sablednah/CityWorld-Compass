package com.sablednah.cityworldcompass.client;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.sablednah.cityworldcompass.Network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.jspecify.annotations.Nullable;

/** Pick what the compass seeks: a search box over every place, shop, district and building the world can hold. */
public final class CompassScreen extends Screen {

    private static final List<String> GROUP_ORDER = List.of("city", "place", "shop", "district", "building");

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33 + 15, 33);
    private final Network.OpenMenu menu;
    private @Nullable Choices choices;
    private @Nullable EditBox search;

    CompassScreen(Network.OpenMenu menu) {
        super(Component.translatable("cityworld_compass.screen.title"));
        this.menu = menu;
    }

    @Override
    protected void init() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title, this.font));
        this.search = header.addChild(new EditBox(this.font, 0, 0, 200, 15, Component.empty()));
        this.search.setHint(Component.translatable("cityworld_compass.screen.search").withStyle(EditBox.SEARCH_HINT_STYLE));
        this.search.setResponder(text -> {
            if (this.choices != null)
                this.choices.filter(text);
        });
        this.choices = this.layout.addToContents(new Choices(this.minecraft));
        LinearLayout buttons = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        buttons.addChild(Button.builder(Component.translatable("cityworld_compass.screen.seek"), b -> this.seek()).build());
        buttons.addChild(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose()).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void setInitialFocus() {
        if (this.search != null)
            this.setInitialFocus(this.search);
        else
            super.setInitialFocus();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.choices != null)
            this.choices.updateSize(this.width, this.layout);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Enter in the search box seeks the highlighted line, or the first line left by the filter. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation() && this.search != null && this.search.isFocused() && this.choices != null) {
            if (this.choices.getSelected() == null && !this.choices.children().isEmpty())
                this.choices.setSelected(this.choices.children().get(0));
            this.seek();
            return true;
        }
        return super.keyPressed(event);
    }

    private void seek() {
        if (this.choices != null && this.choices.getSelected() != null) {
            ClientPacketDistributor.sendToServer(new Network.Seek(this.choices.getSelected().entry.id(), this.menu.mainHand()));
            this.onClose();
        }
    }

    private final class Choices extends ObjectSelectionList<Choices.Line> {

        Choices(Minecraft minecraft) {
            super(minecraft, CompassScreen.this.width, CompassScreen.this.height - 48 - 33, 48, 16);
            filter("");
        }

        void filter(String text) {
            String f = text.toLowerCase(Locale.ROOT).trim();
            List<Line> lines = CompassScreen.this.menu.entries().stream()
                    .filter(e -> f.isEmpty() || e.title().toLowerCase(Locale.ROOT).contains(f)
                            || e.group().contains(f))
                    .sorted(Comparator.comparingInt((Network.Entry e) -> GROUP_ORDER.indexOf(e.group()))
                            .thenComparing(e -> e.title().toLowerCase(Locale.ROOT)))
                    .map(Line::new).toList();
            this.replaceEntries(lines);
            for (Line l : lines)
                if (l.entry.id().equals(CompassScreen.this.menu.current()))
                    this.setSelected(l);
            this.refreshScrollAmount();
        }

        @Override
        public int getRowWidth() {
            return 260;
        }

        final class Line extends ObjectSelectionList.Entry<Line> {
            final Network.Entry entry;
            private final Component title;
            private final Component group;

            Line(Network.Entry entry) {
                this.entry = entry;
                this.title = Component.literal(entry.title());
                this.group = Component.translatable("cityworld_compass.group." + entry.group());
            }

            @Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int y = this.getContentYMiddle() - 9 / 2;
                graphics.drawString(CompassScreen.this.font, this.title, this.getContentX() + 2, y, -1);
                int w = CompassScreen.this.font.width(this.group);
                graphics.drawString(CompassScreen.this.font, this.group, this.getContentX() + Choices.this.getRowWidth() - w - 8, y, 0xFF909090);
            }

            @Override
            public boolean keyPressed(KeyEvent event) {
                if (event.isSelection()) {
                    Choices.this.setSelected(this);
                    CompassScreen.this.seek();
                    return true;
                }
                return super.keyPressed(event);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                Choices.this.setSelected(this);
                if (doubleClick)
                    CompassScreen.this.seek();
                return super.mouseClicked(event, doubleClick);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.title);
            }
        }
    }
}
