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
import java.util.function.Consumer;

/**
 * Create or edit a single calendar event. In a world it submits an {@link EditEventPacket} (the server
 * validates); the preset editor instead passes callbacks that change its in-memory event list.
 */
public class EventEditScreen extends Screen {

    private final Screen parent;
    @Nullable
    private final CalendarEvent existing;
    private final Consumer<CalendarEvent> onSave;
    @Nullable
    private final Consumer<CalendarEvent> onDelete;

    private final List<EventType> types = new ArrayList<>(EventTypes.all());
    private final List<Season> seasons;

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
    private Button saveButton;

    /** Highest valid day: the world's days per season, or the default when editing a preset. */
    private final int maxDay;
    private boolean dayValid = true;
    private boolean yearValid = true;

    private static final int DEFAULT_DAYS_PER_SEASON = 31;
    private static final java.util.function.Predicate<String> DIGITS = value -> value.chars().allMatch(Character::isDigit);

    /** Edits an event in the current world, sending the change to the server. */
    public EventEditScreen(Screen parent, @Nullable CalendarEvent existing, @Nullable ResourceLocation defaultSeason, int defaultDay) {
        this(parent, existing, defaultSeason, defaultDay,
                event -> BetterSeasonsNetwork.sendToServer(existing != null ? EditEventPacket.update(event) : EditEventPacket.add(event)),
                event -> BetterSeasonsNetwork.sendToServer(EditEventPacket.delete(event.getId())));
    }

    /**
     * Edits an event and hands the result to {@code onSave}. {@code onDelete} receives the original event;
     * when null, no Delete button is shown.
     */
    public EventEditScreen(Screen parent, @Nullable CalendarEvent existing, @Nullable ResourceLocation defaultSeason, int defaultDay,
                           Consumer<CalendarEvent> onSave, @Nullable Consumer<CalendarEvent> onDelete) {
        super(Component.translatable("screen." + BettysBetterSeasonsMod.MODID + ".event_edit"));
        this.parent = parent;
        this.existing = existing;
        this.onSave = onSave;
        this.onDelete = onDelete;
        this.seasons = SelectableSeasons.list(existing != null ? existing.getSeason() : null);
        this.maxDay = maxDaysPerSeason();

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

        typeButton = new ShadowlessButton(leftX, ly, LEFT_W, 18, typeLabel(), b -> {
            typeIndex = (typeIndex + 1) % Math.max(1, types.size());
            b.setMessage(typeLabel());
        });
        addRenderableWidget(typeButton);
        ly += BUTTON_ROW;

        seasonButton = new ShadowlessButton(leftX, ly, LEFT_W, 18, seasonLabel(), b -> {
            seasonIndex = (seasonIndex + 1) % Math.max(1, seasons.size());
            b.setMessage(seasonLabel());
        });
        addRenderableWidget(seasonButton);
        int leftBottom = ly + 18;

        // Right column: when the event occurs.
        int ry = topY;
        dayBox = new EditBox(font, rightX, ry, RIGHT_W, 18, Component.literal("Day"));
        dayBox.setMaxLength(3);
        dayBox.setFilter(DIGITS);
        dayBox.setValue(Integer.toString(pendingDay));
        dayBox.setResponder(value -> validate());
        addRenderableWidget(dayBox);
        ry += LABEL_ROW;

        yearBox = new EditBox(font, rightX, ry, RIGHT_W, 18, Component.literal("Year"));
        yearBox.setMaxLength(6);
        yearBox.setFilter(DIGITS);
        yearBox.setValue(Integer.toString(existing != null ? existing.getYear() : currentYear()));
        yearBox.setResponder(value -> validate());
        addRenderableWidget(yearBox);
        ry += LABEL_ROW;

        recurrenceButton = new ShadowlessButton(rightX, ry, RIGHT_W, 18, recurrenceLabel(), b -> {
            recurrence = recurrence == Recurrence.REPEAT_YEARLY ? Recurrence.ONE_TIME : Recurrence.REPEAT_YEARLY;
            b.setMessage(recurrenceLabel());
        });
        addRenderableWidget(recurrenceButton);

        // Action row spans both columns, below whichever column is taller.
        int actionY = Math.max(leftBottom, ry + 18) + 12;
        int halfW = (totalW - 4) / 2;
        saveButton = addRenderableWidget(new ShadowlessButton(leftX, actionY, halfW, 20,
                Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".save"), b -> save()));
        addRenderableWidget(new ShadowlessButton(leftX + halfW + 4, actionY, halfW, 20,
                Component.translatable("gui.cancel"), b -> onClose()));
        if (existing != null && onDelete != null) {
            addRenderableWidget(new ShadowlessButton(leftX, actionY + 24, totalW, 20,
                    Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".delete"), b -> delete()));
        }
        validate();
    }

    /** Checks the day is within the season and a year is entered; Save stays disabled until both hold. */
    private void validate() {
        int day = parseInt(dayBox.getValue(), -1);
        dayValid = day >= 1 && day <= maxDay;
        yearValid = !yearBox.getValue().isEmpty();
        if (saveButton != null) {
            saveButton.active = dayValid && yearValid;
        }
    }

    private void save() {
        if (seasons.isEmpty() || types.isEmpty()) {
            onClose();
            return;
        }
        if (!dayValid || !yearValid) {
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
        onSave.accept(event);
        onClose();
    }

    private void delete() {
        if (existing != null && onDelete != null) {
            onDelete.accept(existing);
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawString(font, this.title, this.width / 2 - font.width(this.title) / 2, this.height / 4 - 20, 0xFFFFFFFF, false);
        drawFieldLabel(graphics, titleBox, fieldLabel("title"), true);
        drawFieldLabel(graphics, descriptionBox, fieldLabel("description"), true);
        drawFieldLabel(graphics, dayBox, Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".field.day_range", maxDay), dayValid);
        drawFieldLabel(graphics, yearBox, fieldLabel("year"), yearValid);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static Component fieldLabel(String key) {
        return Component.translatable("gui." + BettysBetterSeasonsMod.MODID + ".field." + key);
    }

    /** Draws a field's caption above it, in red when the field's value is invalid. */
    private void drawFieldLabel(GuiGraphics graphics, EditBox box, Component label, boolean valid) {
        if (box != null) {
            graphics.drawString(font, label, box.getX(), box.getY() - 10, valid ? 0xFFA0A0A0 : 0xFFFF5555, false);
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

    /** Days per season in the current world, or the default when editing a preset outside a world. */
    private static int maxDaysPerSeason() {
        boolean inWorld = net.minecraft.client.Minecraft.getInstance().level != null && ClientCalendar.getDate() != null;
        return inWorld ? Math.max(1, ClientCalendar.getDaysPerSeason()) : DEFAULT_DAYS_PER_SEASON;
    }

    /** The world's current year, or 1 when editing a preset outside a world. */
    private static int currentYear() {
        CalendarDate date = ClientCalendar.getDate();
        return date != null && net.minecraft.client.Minecraft.getInstance().level != null ? date.year() : 1;
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
