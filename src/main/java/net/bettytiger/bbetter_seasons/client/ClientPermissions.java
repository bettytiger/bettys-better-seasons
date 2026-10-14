package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.bettytiger.bbetter_seasons.calendar.EditPermission;
import net.minecraft.client.Minecraft;

/** Client-side copy of the edit permission mode and this player's op status, for gating editor buttons. */
public final class ClientPermissions {

    private static EditPermission mode = EditPermission.OWNER_ONLY;
    private static boolean isOp = false;

    private ClientPermissions() {
    }

    public static void set(EditPermission mode, boolean isOp) {
        ClientPermissions.mode = mode;
        ClientPermissions.isOp = isOp;
    }

    public static boolean canCreate() {
        return mode.canCreate(isOp);
    }

    public static boolean canModify(CalendarEvent event) {
        var player = Minecraft.getInstance().player;
        return player != null && mode.canModify(isOp, event.getOwner(), player.getUUID());
    }
}
