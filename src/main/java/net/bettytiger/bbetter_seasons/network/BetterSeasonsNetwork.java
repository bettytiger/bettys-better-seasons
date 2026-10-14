package net.bettytiger.bbetter_seasons.network;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** The mod's single play channel. Phase 1 only sends {@link SyncCalendarPacket}. */
@SuppressWarnings("ALL")
public class BetterSeasonsNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(BettysBetterSeasonsMod.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int nextId = 0;

    public static void register() {
        CHANNEL.messageBuilder(SyncCalendarPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCalendarPacket::encode)
                .decoder(SyncCalendarPacket::new)
                .consumerMainThread(SyncCalendarPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSeasonalPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSeasonalPacket::encode)
                .decoder(SyncSeasonalPacket::new)
                .consumerMainThread(SyncSeasonalPacket::handle)
                .add();
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
