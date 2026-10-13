package net.bettytiger.bbetter_seasons.config;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BettysBetterSeasonsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BetterSeasonsConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
}
