package net.bettytiger.bbetter_seasons;

import com.mojang.logging.LogUtils;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsConfig;
import net.bettytiger.bbetter_seasons.network.BetterSeasonsNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@SuppressWarnings("ALL")
@Mod(BettysBetterSeasonsMod.MODID)
public class BettysBetterSeasonsMod {
    public static final String MODID = "bbetter_seasons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BettysBetterSeasonsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BetterSeasonsConfig.SERVER_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(BetterSeasonsNetwork::register);
    }
}
