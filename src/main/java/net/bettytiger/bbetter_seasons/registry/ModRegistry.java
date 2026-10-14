package net.bettytiger.bbetter_seasons.registry;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.block.WitheredCropBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Blocks and items registered by the library. */
public class ModRegistry {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BettysBetterSeasonsMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BettysBetterSeasonsMod.MODID);

    /** Soils tagged with this treat crops planted on them as always in season (greenhouse soil). */
    public static final TagKey<Block> IGNORES_SEASONS =
            TagKey.create(Registries.BLOCK, new ResourceLocation(BettysBetterSeasonsMod.MODID, "ignores_seasons"));

    public static final RegistryObject<Block> WITHERED_CROP = BLOCKS.register("withered_crop",
            () -> new WitheredCropBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .randomTicks()));

    public static final RegistryObject<Item> WITHERED_CROP_ITEM = ITEMS.register("withered_crop",
            () -> new BlockItem(WITHERED_CROP.get(), new Item.Properties()));

    private ModRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
