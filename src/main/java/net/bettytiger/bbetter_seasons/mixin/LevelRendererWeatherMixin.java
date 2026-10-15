package net.bettytiger.bbetter_seasons.mixin;

import net.bettytiger.bbetter_seasons.weather.SeasonalWeather;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * During a snow season, draws snowflakes instead of rain and skips rain splashes and sounds. Optional
 * (require = 0) so a mod that replaces weather rendering doesn't stop the game from loading.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererWeatherMixin {

    @Inject(method = {"renderSnowAndRain", "tickRain"}, at = @At("HEAD"), require = 0)
    private void bbetter_seasons$beginSnow(CallbackInfo ci) {
        SeasonalWeather.beginConversion(SeasonalWeather.isClientSnowing());
    }

    @Inject(method = {"renderSnowAndRain", "tickRain"}, at = @At("RETURN"), require = 0)
    private void bbetter_seasons$endSnow(CallbackInfo ci) {
        SeasonalWeather.endConversion();
    }
}
