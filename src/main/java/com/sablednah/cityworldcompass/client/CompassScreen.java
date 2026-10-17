package com.sablednah.cityworldcompass.client;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.sablednah.cityworldcompass.Network;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Pick what the compass seeks: a search box over every place, shop, district and building the world can hold.
 * (1.20.1 has no header-and-footer layout, so the screen is laid out by hand: title, search box, list, buttons.)
 */
public final class CompassScreen extends Screen {

    private static final List<String> GROUP_ORDER = List.of("city", "place", "shop", "district", "building");
    private static final int TOP = 48, BOTTOM = 33;

    private final Network.OpenMenu menu;
    private @Nullable Choices choices;
    private @Nullable EditBox search;

    CompassScreen(Network.OpenMenu menu) {
        super(Component.translatable("cityworld_compass.screen.title"));
        this.menu = menu;
    }

    @Override
    protected void init() {
        String text = this.search == null ? "" : this.search.getValue();
        this.search = new EditBox(this.font, this.width / 2 - 100, 22, 200, 15, Component.empty());
        this.search.setHint(Component.translatable("cityworld_compass.screen.search").withStyle(ChatFormatting.DARK_GRAY));
        this.search.setValue(text);
        this.search.setResponder(t -> {
            if (this.choices != null)
                this.choices.filter(t);
        });
        this.choices = new Choices(this.minecraft);
        this.choices.filter(text);
        this.addRenderableWidget(this.choices);
        this.addRenderableWidget(this.search);
        int y = this.height - BOTTOM + 6;
        this.addRenderableWidget(Button.builder(Component.translatable("cityworld_compass.screen.seek"), b -> this.seek())
                .bounds(this.width / 2 - 154, y, 150, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(this.width / 2 + 4, y, 150, 20).build());
        this.setInitialFocus(this.search);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Enter in the search box seeks the highlighted line, or the first line left by the filter. */
    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && this.search != null && this.search.isFocused()
                && this.choices != null) {
            if (this.choices.getSelected() == null && !this.choices.children().isEmpty())
                this.choices.setSelected(this.choices.children().get(0));
            this.seek();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void seek() {
        if (this.choices != null && this.choices.getSelected() != null) {
            Network.sendToServer(new Network.Seek(this.choices.getSelected().entry.id(), this.menu.mainHand()));
            this.onClose();
        }
    }

    private final class Choices extends ObjectSelectionList<Choices.Line> {

        Choices(Minecraft minecraft) {
            super(minecraft, CompassScreen.this.width, CompassScreen.this.height, TOP, CompassScreen.this.height - BOTTOM, 16);
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
            this.setScrollAmount(0);
        }

        @Override
        public int getRowWidth() {
            return 260;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 140;
        }

        final class Line extends ObjectSelectionList.Entry<Line> {
            final Network.Entry entry;
            private final Component title;
            private final Component group;
            private long lastClick;

            Line(Network.Entry entry) {
                this.entry = entry;
                this.title = Component.literal(entry.title());
                this.group = Component.translatable("cityworld_compass.group." + entry.group());
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX,
                    int mouseY, boolean hovered, float a) {
                int y = top + height / 2 - 9 / 2;
                graphics.drawString(CompassScreen.this.font, this.title, left + 2, y, 0xFFFFFF);
                int w = CompassScreen.this.font.width(this.group);
                graphics.drawString(CompassScreen.this.font, this.group, left + width - w - 8, y, 0x909090);
            }

            @Override
            public boolean keyPressed(int key, int scan, int modifiers) {
                if (CommonInputs.selected(key)) {
                    Choices.this.setSelected(this);
                    CompassScreen.this.seek();
                    return true;
                }
                return super.keyPressed(key, scan, modifiers);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                Choices.this.setSelected(this);
                if (Util.getMillis() - this.lastClick < 250L)
                    CompassScreen.this.seek();
                this.lastClick = Util.getMillis();
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.title);
            }
        }
    }
}
