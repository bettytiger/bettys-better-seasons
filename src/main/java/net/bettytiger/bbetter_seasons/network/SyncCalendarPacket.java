package net.bettytiger.bbetter_seasons.network;

import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.client.ClientCalendar;
import net.bettytiger.bbetter_seasons.client.ClientWeather;
import net.bettytiger.bbetter_seasons.client.SeasonalTint;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server to client: the full calendar snapshot. Sent on login, on {@code /reload}, on a day crossing,
 * and after a {@code /calendar} or {@code /season} command. The client recomputes the date from these
 * fields so both sides share one implementation of the math.
 */
public class SyncCalendarPacket {

    private final long absDay;
    private final int daysPerSeason;
    private final int ticksPerDay;
    private final int startYear;
    private final int earlyEnd;
    private final int midEnd;
    private final List<Season> seasons;
    /** Dimensions that follow the seasons (server config), so the client only tints those. */
    private final List<String> seasonalDimensions;
    /** Whether seasons apply their weather (server config), so the client knows when rain should fall as snow. */
    private final boolean seasonalWeather;

    public SyncCalendarPacket(long absDay, int daysPerSeason, int ticksPerDay, int startYear, int earlyEnd, int midEnd,
                              List<Season> seasons, List<String> seasonalDimensions, boolean seasonalWeather) {
        this.absDay = absDay;
        this.daysPerSeason = daysPerSeason;
        this.ticksPerDay = ticksPerDay;
        this.startYear = startYear;
        this.earlyEnd = earlyEnd;
        this.midEnd = midEnd;
        this.seasons = seasons;
        this.seasonalDimensions = seasonalDimensions;
        this.seasonalWeather = seasonalWeather;
    }

    public SyncCalendarPacket(FriendlyByteBuf buf) {
        this.absDay = buf.readLong();
        this.daysPerSeason = buf.readVarInt();
        this.ticksPerDay = buf.readVarInt();
        this.startYear = buf.readVarInt();
        this.earlyEnd = buf.readVarInt();
        this.midEnd = buf.readVarInt();
        int count = buf.readVarInt();
        List<Season> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(Season.readFrom(buf));
        }
        this.seasons = list;
        int dimensionCount = buf.readVarInt();
        List<String> dimensions = new ArrayList<>(dimensionCount);
        for (int i = 0; i < dimensionCount; i++) {
            dimensions.add(buf.readUtf());
        }
        this.seasonalDimensions = dimensions;
        this.seasonalWeather = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(absDay);
        buf.writeVarInt(daysPerSeason);
        buf.writeVarInt(ticksPerDay);
        buf.writeVarInt(startYear);
        buf.writeVarInt(earlyEnd);
        buf.writeVarInt(midEnd);
        buf.writeVarInt(seasons.size());
        for (Season season : seasons) {
            season.writeTo(buf);
        }
        buf.writeVarInt(seasonalDimensions.size());
        for (String dimension : seasonalDimensions) {
            buf.writeUtf(dimension);
        }
        buf.writeBoolean(seasonalWeather);
    }

    public List<String> seasonalDimensions() {
        return seasonalDimensions;
    }

    public boolean seasonalWeather() {
        return seasonalWeather;
    }

    public long absDay() {
        return absDay;
    }

    public int daysPerSeason() {
        return daysPerSeason;
    }

    public int ticksPerDay() {
        return ticksPerDay;
    }

    public int startYear() {
        return startYear;
    }

    public int earlyEnd() {
        return earlyEnd;
    }

    public int midEnd() {
        return midEnd;
    }

    public List<Season> seasons() {
        return seasons;
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // Guard with DistExecutor so the client-only store is never touched on a dedicated server.
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientCalendar.accept(this);
            SeasonalTint.refresh();
            ClientWeather.refresh();
        }));
        context.setPacketHandled(true);
    }
}
