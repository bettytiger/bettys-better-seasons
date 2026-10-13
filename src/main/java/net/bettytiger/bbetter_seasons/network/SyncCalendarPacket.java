package net.bettytiger.bbetter_seasons.network;

import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.client.ClientCalendar;
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
    private final int startYear;
    private final int earlyEnd;
    private final int midEnd;
    private final List<Season> seasons;

    public SyncCalendarPacket(long absDay, int daysPerSeason, int startYear, int earlyEnd, int midEnd, List<Season> seasons) {
        this.absDay = absDay;
        this.daysPerSeason = daysPerSeason;
        this.startYear = startYear;
        this.earlyEnd = earlyEnd;
        this.midEnd = midEnd;
        this.seasons = seasons;
    }

    public SyncCalendarPacket(FriendlyByteBuf buf) {
        this.absDay = buf.readLong();
        this.daysPerSeason = buf.readVarInt();
        this.startYear = buf.readVarInt();
        this.earlyEnd = buf.readVarInt();
        this.midEnd = buf.readVarInt();
        int count = buf.readVarInt();
        List<Season> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(Season.readFrom(buf));
        }
        this.seasons = list;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(absDay);
        buf.writeVarInt(daysPerSeason);
        buf.writeVarInt(startYear);
        buf.writeVarInt(earlyEnd);
        buf.writeVarInt(midEnd);
        buf.writeVarInt(seasons.size());
        for (Season season : seasons) {
            season.writeTo(buf);
        }
    }

    public long absDay() {
        return absDay;
    }

    public int daysPerSeason() {
        return daysPerSeason;
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
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCalendar.accept(this)));
        context.setPacketHandled(true);
    }
}
