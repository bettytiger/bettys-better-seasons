package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.seasonal.SeasonalCropInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Client-side mirror of the seasonal crop data, keyed by registry id. Datapacks only exist on the
 * server, so this is synced over the network for "Grows in" tooltips to work on dedicated servers.
 */
public final class ClientSeasonal {

    private static Map<ResourceLocation, SeasonalCropInfo> byId = Map.of();

    private ClientSeasonal() {
    }

    public static void set(Map<ResourceLocation, SeasonalCropInfo> data) {
        byId = data;
    }

    @Nullable
    public static SeasonalCropInfo getForItem(Item item) {
        return byId.get(ForgeRegistries.ITEMS.getKey(item));
    }

    @Nullable
    public static SeasonalCropInfo getForBlock(Block block) {
        return byId.get(ForgeRegistries.BLOCKS.getKey(block));
    }
}
