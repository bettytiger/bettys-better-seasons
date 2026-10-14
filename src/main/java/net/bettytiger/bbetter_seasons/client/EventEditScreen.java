package net.bettytiger.bbetter_seasons.client;

import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.calendar.*;
import net.bettytiger.bbetter_seasons.network.BetterSeasonsNetwork;
import net.bettytiger.bbetter_seasons.network.EditEventPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Create or edit a single calendar event. Submits an {@link EditEventPacket}; the server validates. */
public class EventEditScreen extends Screen {

    private final Screen parent;
    @Nullable
    private final CalendarEvent existing;

    private final List<EventType> types = new ArrayList<>(EventTypes.all());
    private final List<Season> seasons = ClientCalendar.getSeasons();

    private int typeIndex;
    private int seasonIndex;
    private Recurrence recurrence = Recurrence.REPEAT_YEARLY;

    private EditBox titleBox;
    private EditBox descriptionBox;
    private EditBox dayBox;
    private EditBox yearBox;
    private Button typeButton;
    private Button seasonButton;
    private Button recurrenceButton;

    public EventEditScreen(Screen parent, @Nullable CalendarEvent existing, @Nullable ResourceLocation defaultSeason, int defaultDay) {
        super(Component.translatable("screen." + BettysBetterSeasonsMod.MODID + ".event_edit"));
        this.parent = parent;
        this.existing = existing;

        if (existing != null) {
            typeIndex = Math.max(0, indexOfType(existing.getType()));
            seasonIndex = Math.max(0, indexOfSeason(existing.getSeason()));
            recurrence = existing.getRecurrence();
        } else {
            seasonIndex = Math.max(0, indexOfSeason(defaultSeason));
        }
        this.pendingDay = existing != null ? existing.getDay() : Math.max(1, defaultDay);
    }

    private final int pendingDay;

    private static final int LEFT_W = 160;
    private static final int RIGHT_W = 110;
    private static final int COL_GAP = 12;
    private static final int LABEL_ROW = 30;
    private static final int BUTTON_ROW = 22;

    @Override
    protected void init() {
        int cx = this.width / 2;
        int totalW = LEFT_W + COL_GAP + RIGHT_W;
        int leftX = cx - totalW / 2;
        int rightX = leftX + LEFT_W + COL_GAP;
        int topY = this.height / 4;

        // Left column: the event's identity.
        int ly = topY;
        titleBox = new EditBox(font, leftX, ly, LEFT_W, 18, Component.literal("Title"));
        titleBox.setMaxLength(64);
        titleBox.setValue(existing != null ? existing.getTitle() : "New Event");
        addRenderableWidget(titleBox);
        ly += LABEL_ROW;

        descriptionBox = new EditBox(font, leftX, ly, LEFT_W, 18, Component.literal("Description"));
        descriptionBox.setMaxLength(256);
        descriptionBox.setValue(existing != null ? existing.getDescription() : "");
        addRenderableWidget(descriptionBox);
        ly += LABEL_ROW;

        typeButton = Button.builder(typeLabel(), b -> {
            typeIndex = (typeIndex + 1) % Math.max(1, types.size());
            b.setMessage(typeLabel());
        }).bounds(leftX, ly, LEFT_W, 18).build();
        addRenderableWidget(typeButton);
        ly += BUTTON_ROW;

        seasonButton = Button.builder(seasonLabel(), b -> {
            seasonIndex = (seasonIndex + 1) % Math.max(1, seasons.size());
            b.setMessage(seasonLabel());
        }).bounds(leftX, ly, LEFT_W, 18).build();
        addRenderableWidget(seasonButton);
        int leftBottom = ly + 18;

        // Right column: when the event occurs.
        int ry = topY;
        dayBox = new EditBox(font, rightX, ry, RIGHT_W, 18, Component.literal("Day"));
        dayBox.setValue(Integer.toString(pendingDay));
        addRenderableWidget(dayBox);
        ry += LABEL_ROW;

        yearBox = new EditBox(font, rightX, ry, RIGHT_W, 18, Component.literal("Year"));
        yearBox.setValue(Integer.toString(existing != null ? existing.getYear() : currentYear()));
        addRenderableWidget(yearBox);
        ry += LABEL_ROW;

        recurrenceButton = Button.builder(recurrenceLabel(), b -> {
            recurrence = recurrence == Recurrence.REPEAT_YEARLY ? Recurrence.ONE_TIME : Recurrence.REPEAT_YEARLY;
            b.setMessage(recurrenceLabel());
        }).bounds(rightX, ry, RIGHT_W, 18).build();
        addRenderableWidget(recurrenceButton);

        // Action row spans both columns, below whichever column is taller.
        int actionY = Math.max(leftBottom, ry + 18) + 12;
        int halfW = (totalW - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".save"), b -> save())
                .bounds(leftX, actionY, halfW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(leftX + halfW + 4, actionY, halfW, 20).build());
        if (existing != null) {
            addRenderableWidget(Button.builder(Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".delete"), b -> delete())
                    .bounds(leftX, actionY + 24, totalW, 20).build());
        }
    }

    private void save() {
        if (seasons.isEmpty() || types.isEmpty()) {
            onClose();
            return;
        }
        UUID id = existing != null ? existing.getId() : UUID.randomUUID();
        ResourceLocation type = types.get(typeIndex).id();
        ResourceLocation season = seasons.get(seasonIndex).id();
        int day = parseInt(dayBox.getValue(), 1);
        int year = parseInt(yearBox.getValue(), currentYear());
        UUID owner = existing != null ? existing.getOwner() : ownUuid();
        CompoundTag data = existing != null ? existing.getData() : new CompoundTag();

        CalendarEvent event = new CalendarEvent(id, type, titleBox.getValue(), descriptionBox.getValue(),
                season, day, recurrence, year, null, null, owner, data);
        BetterSeasonsNetwork.sendToServer(existing != null ? EditEventPacket.update(event) : EditEventPacket.add(event));
        onClose();
    }

    private void delete() {
        if (existing != null) {
            BetterSeasonsNetwork.sendToServer(EditEventPacket.delete(existing.getId()));
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, this.title, this.width / 2, this.height / 4 - 20, 0xFFFFFFFF);
        drawFieldLabel(graphics, titleBox, "title");
        drawFieldLabel(graphics, descriptionBox, "description");
        drawFieldLabel(graphics, dayBox, "day");
        drawFieldLabel(graphics, yearBox, "year");
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFieldLabel(GuiGraphics graphics, EditBox box, String key) {
        if (box != null) {
            graphics.drawString(font, Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".field." + key),
                    box.getX(), box.getY() - 10, 0xFFA0A0A0);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private Component typeLabel() {
        Component name = types.isEmpty() ? Component.literal("—") : Component.translatable(types.get(typeIndex).translationKey());
        return Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".type").append(": ").append(name);
    }

    private Component seasonLabel() {
        Component name = seasons.isEmpty() ? Component.literal("—") : seasons.get(seasonIndex).displayName();
        return Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".season").append(": ").append(name);
    }

    private Component recurrenceLabel() {
        String key = recurrence == Recurrence.REPEAT_YEARLY ? "recurrence.yearly" : "recurrence.once";
        return Component.translatable("gui." + BettysBetterSeasonsMod.MODID + "." + key);
    }

    private int indexOfType(ResourceLocation id) {
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i).id().equals(id)) {
                return i;
            }
        }
        return 0;
    }

    private int indexOfSeason(@Nullable ResourceLocation id) {
        if (id != null) {
            for (int i = 0; i < seasons.size(); i++) {
                if (seasons.get(i).id().equals(id)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private static int currentYear() {
        CalendarDate date = ClientCalendar.getDate();
        return date != null ? date.year() : 1;
    }

    private static UUID ownUuid() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        return player != null ? player.getUUID() : null;
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
