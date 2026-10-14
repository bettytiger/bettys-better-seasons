package net.bettytiger.bbetter_seasons.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A vanilla-looking button whose label is drawn without a drop shadow. */
public class ShadowlessButton extends Button {

    public ShadowlessButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderScrollingString(GuiGraphics graphics, Font font, int padding, int color) {
        Component message = getMessage();
        int textWidth = font.width(message);
        int available = getWidth() - padding * 2;
        int x = textWidth > available
                ? getX() + padding
                : getX() + (getWidth() - textWidth) / 2;
        int y = getY() + (getHeight() - font.lineHeight) / 2;
        graphics.drawString(font, message, x, y, color, false);
    }
}
