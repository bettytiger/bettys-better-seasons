package net.bettytiger.bbetter_seasons.calendar;

import java.util.UUID;

/** Who may create, edit and delete calendar events. Configured server-side. */
public enum EditPermission {
    /** Players manage their own events; world events (no owner) are op-only. The default. */
    OWNER_ONLY,
    /** Only operators may edit anything. */
    OP,
    /** Anyone may edit anything. */
    ALL;

    public boolean canCreate(boolean isOp) {
        return switch (this) {
            case OP -> isOp;
            case ALL, OWNER_ONLY -> true;
        };
    }

    public boolean canModify(boolean isOp, UUID owner, UUID player) {
        if (isOp) {
            return true;
        }
        return switch (this) {
            case OP -> false;
            case ALL -> true;
            case OWNER_ONLY -> owner != null && owner.equals(player);
        };
    }
}
