package net.bettytiger.bbetter_seasons.mixin;

import net.bettytiger.bbetter_seasons.weather.SeasonalWeather;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** During a snow season, precipitation ticks fill cauldrons with powder snow instead of water. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelWeatherMixin {

    // Optional (require = 0): only cauldron filling depends on it, so another mod replacing this method is fine.
    @Inject(method = "tickChunk", at = @At("HEAD"), require = 0)
    private void bbetter_seasons$beginSnow(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        SeasonalWeather.beginConversion(SeasonalWeather.isSnowing((ServerLevel) (Object) this));
    }

    @Inject(method = "tickChunk", at = @At("RETURN"), require = 0)
    private void bbetter_seasons$endSnow(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        SeasonalWeather.endConversion();
    }
}
