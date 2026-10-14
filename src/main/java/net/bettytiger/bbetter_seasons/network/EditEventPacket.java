package net.bettytiger.bbetter_seasons.network;

import net.bettytiger.bbetter_seasons.calendar.CalendarEvent;
import net.bettytiger.bbetter_seasons.calendar.CalendarEventSavedData;
import net.bettytiger.bbetter_seasons.calendar.CalendarManager;
import net.bettytiger.bbetter_seasons.calendar.EditPermission;
import net.bettytiger.bbetter_seasons.config.BetterSeasonsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client to server: create, update, or delete a calendar event. The server re-checks permission
 * before applying anything and then re-syncs the event list to all players.
 */
public class EditEventPacket {

    public enum Op {ADD, UPDATE, DELETE}

    private final Op op;
    private final CompoundTag eventTag;
    private final UUID targetId;

    private EditEventPacket(Op op, CompoundTag eventTag, UUID targetId) {
        this.op = op;
        this.eventTag = eventTag;
        this.targetId = targetId;
    }

    public static EditEventPacket add(CalendarEvent event) {
        return new EditEventPacket(Op.ADD, event.save(), event.getId());
    }

    public static EditEventPacket update(CalendarEvent event) {
        return new EditEventPacket(Op.UPDATE, event.save(), event.getId());
    }

    public static EditEventPacket delete(UUID id) {
        return new EditEventPacket(Op.DELETE, new CompoundTag(), id);
    }

    public EditEventPacket(FriendlyByteBuf buf) {
        this.op = buf.readEnum(Op.class);
        this.eventTag = buf.readNbt();
        this.targetId = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(op);
        buf.writeNbt(eventTag != null ? eventTag : new CompoundTag());
        buf.writeUUID(targetId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            CalendarManager manager = CalendarManager.get();
            if (sender == null || manager == null) {
                return;
            }
            apply(sender, manager);
        });
        context.setPacketHandled(true);
    }

    private void apply(ServerPlayer sender, CalendarManager manager) {
        EditPermission mode = BetterSeasonsConfig.SERVER.eventEditPermission.get();
        boolean isOp = sender.hasPermissions(2);
        CalendarEventSavedData data = CalendarEventSavedData.get(manager.overworld());

        boolean changed = false;
        switch (op) {
            case ADD -> {
                if (mode.canCreate(isOp)) {
                    CalendarEvent event = CalendarEvent.load(eventTag);
                    // Non-ops may only create events owned by themselves.
                    if (!isOp && mode != EditPermission.ALL) {
                        event = withOwner(event, sender.getUUID());
                    }
                    data.add(event);
                    changed = true;
                }
            }
            case UPDATE -> {
                CalendarEvent existing = data.get(targetId);
                if (existing != null && mode.canModify(isOp, existing.getOwner(), sender.getUUID())) {
                    CalendarEvent updated = CalendarEvent.load(eventTag);
                    if (!isOp && mode != EditPermission.ALL) {
                        updated = withOwner(updated, existing.getOwner());
                    }
                    data.remove(targetId);
                    data.add(updated);
                    changed = true;
                }
            }
            case DELETE -> {
                CalendarEvent existing = data.get(targetId);
                if (existing != null && mode.canModify(isOp, existing.getOwner(), sender.getUUID())) {
                    changed = data.remove(targetId);
                }
            }
        }

        if (changed) {
            manager.syncEventsAll();
        }
    }

    private static CalendarEvent withOwner(CalendarEvent event, UUID owner) {
        CompoundTag tag = event.save();
        if (owner != null) {
            tag.putUUID("owner", owner);
        } else {
            tag.remove("owner");
        }
        return CalendarEvent.load(tag);
    }
}
