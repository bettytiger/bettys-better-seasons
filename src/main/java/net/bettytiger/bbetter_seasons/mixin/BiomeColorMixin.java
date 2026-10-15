package net.bettytiger.bbetter_seasons.mixin;

import net.bettytiger.bbetter_seasons.client.SeasonalTint;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tints biome grass and foliage colours with the season. Runs after vanilla and Forge colour modifiers
 * (swamp, dark forest), so biome character is kept and only blended toward the season's tint.
 */
@Mixin(Biome.class)
public abstract class BiomeColorMixin {

    @Inject(method = "getGrassColor(DD)I", at = @At("RETURN"), cancellable = true)
    private void bbetter_seasons$seasonalGrass(double x, double z, CallbackInfoReturnable<Integer> cir) {
        int vanilla = cir.getReturnValue();
        int tinted = SeasonalTint.apply(vanilla);
        if (tinted != vanilla) {
            cir.setReturnValue(tinted);
        }
    }

    @Inject(method = "getFoliageColor()I", at = @At("RETURN"), cancellable = true)
    private void bbetter_seasons$seasonalFoliage(CallbackInfoReturnable<Integer> cir) {
        int vanilla = cir.getReturnValue();
        int tinted = SeasonalTint.apply(vanilla);
        if (tinted != vanilla) {
            cir.setReturnValue(tinted);
        }
    }
}
