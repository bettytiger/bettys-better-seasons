package net.bettytiger.bbetter_seasons.network;

import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.bettytiger.bbetter_seasons.client.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server to client: the full calendar event list, for the calendar screen. */
public class SyncEventsPacket {

    private final List<CalendarEvent> events;

    public SyncEventsPacket(List<CalendarEvent> events) {
        this.events = events;
    }

    public SyncEventsPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<CalendarEvent> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(CalendarEvent.readFrom(buf));
        }
        this.events = list;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(events.size());
        for (CalendarEvent event : events) {
            event.writeTo(buf);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEvents.set(this.events)));
        context.setPacketHandled(true);
    }
}
