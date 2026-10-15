package net.bettytiger.bbetter_seasons.mixin;

import net.bettytiger.bbetter_seasons.weather.SeasonalWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While rain falls as snow it isn't "raining" at a position, as in vanilla snowy biomes: fires stay lit,
 * lightning doesn't strike, and mobs don't get wet.
 */
@Mixin(Level.class)
public abstract class LevelWeatherMixin {

    @Inject(method = "isRainingAt", at = @At("RETURN"), cancellable = true)
    private void bbetter_seasons$snowIsNotRain(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && SeasonalWeather.isSnowing((Level) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
