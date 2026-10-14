package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.preset.PresetSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Adds the "Calendar Preset" button to the Create World screen's Game tab (called from a mixin). */
public final class CreateWorldPresetButton {

    private static final String KEY = "gui." + BettysBetterSeasonsMod.MODID + ".preset.";

    private CreateWorldPresetButton() {
    }

    public static void addTo(GridLayout layout, Screen createWorldScreen) {
        PresetSelection.beginSession(createWorldScreen);

        // The Game tab is a single-column grid with one child per row; append below the existing rows.
        int[] nextRow = {0};
        layout.visitChildren(child -> nextRow[0]++);

        PresetButton button = new PresetButton(b -> Minecraft.getInstance().setScreen(new PresetListScreen(createWorldScreen)));
        button.setTooltip(Tooltip.create(Component.translatable(KEY + "button.tooltip")));
        layout.addChild(button, nextRow[0], 0);
    }

    private static Component label() {
        String chosen = PresetSelection.selectedName();
        return Component.translatable(KEY + "button",
                chosen != null ? Component.literal(chosen) : Component.translatable(KEY + "none"));
    }

    /**
     * Reads its label from the current selection each frame. Returning to the Create World screen only
     * repositions its widgets instead of rebuilding them, so a label set once would go stale.
     */
    private static final class PresetButton extends Button {

        PresetButton(OnPress onPress) {
            super(0, 0, 210, 20, label(), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            setMessage(label());
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }
}
