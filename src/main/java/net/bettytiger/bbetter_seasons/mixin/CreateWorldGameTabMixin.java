package net.bettytiger.bbetter_seasons.mixin;

import net.bettytiger.bbetter_seasons.client.CreateWorldPresetButton;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the calendar preset button to the Game tab of the Create World screen. */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
public abstract class CreateWorldGameTabMixin extends GridLayoutTab {

    protected CreateWorldGameTabMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbetter_seasons$addPresetButton(CreateWorldScreen screen, CallbackInfo ci) {
        CreateWorldPresetButton.addTo(this.layout, screen);
    }
}
