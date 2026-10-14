package net.bettytiger.bbetter_seasons.network;

import net.bettytiger.bbetter_seasons.calendar.Season;
import net.bettytiger.bbetter_seasons.client.ClientCalendar;
import net.bettytiger.bbetter_seasons.client.SeasonTransition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to client: play the season transition. A normal change fades old -&gt; new; an intro
 * (first entry into a world) shows "Year N Season" with no previous season.
 */
public class SeasonTransitionPacket {

    private final ResourceLocation oldSeason;
    private final ResourceLocation newSeason;
    private final boolean intro;
    private final int year;

    private SeasonTransitionPacket(ResourceLocation oldSeason, ResourceLocation newSeason, boolean intro, int year) {
        this.oldSeason = oldSeason;
        this.newSeason = newSeason;
        this.intro = intro;
        this.year = year;
    }

    public static SeasonTransitionPacket change(ResourceLocation oldSeason, ResourceLocation newSeason) {
        return new SeasonTransitionPacket(oldSeason, newSeason, false, 0);
    }

    public static SeasonTransitionPacket intro(ResourceLocation season, int year) {
        return new SeasonTransitionPacket(season, season, true, year);
    }

    public SeasonTransitionPacket(FriendlyByteBuf buf) {
        this.oldSeason = buf.readResourceLocation();
        this.newSeason = buf.readResourceLocation();
        this.intro = buf.readBoolean();
        this.year = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(oldSeason);
        buf.writeResourceLocation(newSeason);
        buf.writeBoolean(intro);
        buf.writeVarInt(year);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::playClient));
        context.setPacketHandled(true);
    }

    private void playClient() {
        Season newS = find(newSeason);
        Component newName = newS != null ? newS.displayName() : Component.literal(newSeason.getPath());
        int color = newS != null ? newS.color() : 0xFFFFFF;
        if (intro) {
            SeasonTransition.startIntro(newName, color, year);
        } else {
            Season oldS = find(oldSeason);
            Component oldName = oldS != null ? oldS.displayName() : Component.literal(oldSeason.getPath());
            SeasonTransition.start(oldName, newName, color);
        }
    }

    private static Season find(ResourceLocation id) {
        for (Season season : ClientCalendar.getSeasons()) {
            if (season.id().equals(id)) {
                return season;
            }
        }
        return null;
    }
}
