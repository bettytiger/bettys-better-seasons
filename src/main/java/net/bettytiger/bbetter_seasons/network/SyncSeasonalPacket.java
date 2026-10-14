package net.bettytiger.bbetter_seasons.network;

import net.bettytiger.bbetter_seasons.client.ClientSeasonal;
import net.bettytiger.bbetter_seasons.seasonal.SeasonalCropInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Server to client: the resolved seasonal crop data, keyed by block/item registry id, for tooltips.
 * Sent alongside the calendar sync on login and {@code /reload}.
 */
public class SyncSeasonalPacket {

    private final Map<ResourceLocation, SeasonalCropInfo> data;

    public SyncSeasonalPacket(Map<ResourceLocation, SeasonalCropInfo> data) {
        this.data = data;
    }

    public SyncSeasonalPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<ResourceLocation, SeasonalCropInfo> map = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            map.put(id, SeasonalCropInfo.readFrom(buf));
        }
        this.data = map;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(data.size());
        for (Map.Entry<ResourceLocation, SeasonalCropInfo> entry : data.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            entry.getValue().writeTo(buf);
        }
    }

    public Map<ResourceLocation, SeasonalCropInfo> data() {
        return data;
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSeasonal.set(this.data)));
        context.setPacketHandled(true);
    }
}
