package net.bettytiger.bbetter_seasons.network;

import net.bettytiger.bbetter_seasons.calendar.EditPermission;
import net.bettytiger.bbetter_seasons.client.ClientPermissions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: the edit permission mode and whether this player is an operator. */
public class SyncPermissionPacket {

    private final EditPermission mode;
    private final boolean isOp;

    public SyncPermissionPacket(EditPermission mode, boolean isOp) {
        this.mode = mode;
        this.isOp = isOp;
    }

    public SyncPermissionPacket(FriendlyByteBuf buf) {
        this.mode = buf.readEnum(EditPermission.class);
        this.isOp = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(mode);
        buf.writeBoolean(isOp);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPermissions.set(mode, isOp)));
        context.setPacketHandled(true);
    }
}
