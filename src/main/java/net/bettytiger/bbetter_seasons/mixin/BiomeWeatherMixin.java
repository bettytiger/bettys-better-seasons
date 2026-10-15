package net.bettytiger.bbetter_seasons.mixin;

import net.bettytiger.bbetter_seasons.weather.SeasonalWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns rain into snow while a snow season is active. Only applies inside a conversion started by one of the
 * weather mixins, because a biome alone doesn't know which dimension (or side) is asking.
 */
@Mixin(Biome.class)
public abstract class BiomeWeatherMixin {

    @Inject(method = "getPrecipitationAt", at = @At("RETURN"), cancellable = true)
    private void bbetter_seasons$seasonalSnow(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        Biome.Precipitation vanilla = cir.getReturnValue();
        Biome.Precipitation seasonal = SeasonalWeather.convert(vanilla);
        if (seasonal != vanilla) {
            cir.setReturnValue(seasonal);
        }
    }
}
