package net.bettytiger.bbetter_seasons.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Server config for the calendar. Every rule in the Phase 1 calendar model is configurable here.
 * Client config (HUD, tooltips, transition screen) arrives in later phases.
 */
public class BetterSeasonsConfig {

    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        Pair<Server, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = pair.getLeft();
        SERVER_SPEC = pair.getRight();
    }

    public static class Server {
        public final ForgeConfigSpec.IntValue daysPerSeason;
        public final ForgeConfigSpec.IntValue ticksPerDay;
        public final ForgeConfigSpec.IntValue startYear;
        public final ForgeConfigSpec.IntValue subSeasonEarlyEnd;
        public final ForgeConfigSpec.IntValue subSeasonMidEnd;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> seasonalDimensions;

        public final ForgeConfigSpec.BooleanValue seasonalCropsEnabled;
        public final ForgeConfigSpec.DoubleValue slowFactor;
        public final ForgeConfigSpec.BooleanValue witherDropsSeed;
        public final ForgeConfigSpec.EnumValue<net.bettytiger.bbetter_seasons.seasonal.OutOfSeasonRule> defaultRule;

        public final ForgeConfigSpec.BooleanValue chatNotifications;
        public final ForgeConfigSpec.IntValue notificationLeadDays;

        public final ForgeConfigSpec.EnumValue<net.bettytiger.bbetter_seasons.calendar.EditPermission> eventEditPermission;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Calendar rules. Changing days per season or season count shifts how existing dates read.")
                    .push("calendar");

            daysPerSeason = builder
                    .comment("Number of days in every season.")
                    .defineInRange("daysPerSeason", 31, 1, 100_000);

            ticksPerDay = builder
                    .comment("Ticks per calendar day. 24000 = one Minecraft day.")
                    .defineInRange("ticksPerDay", 24000, 1, 1_000_000);

            startYear = builder
                    .comment("The year the calendar starts counting from.")
                    .defineInRange("startYear", 1, 0, 1_000_000);

            subSeasonEarlyEnd = builder
                    .comment("Last day (inclusive) of the Early sub-season.")
                    .defineInRange("subSeasonEarlyEnd", 10, 1, 100_000);

            subSeasonMidEnd = builder
                    .comment("Last day (inclusive) of the Mid sub-season. Days after this are Late.")
                    .defineInRange("subSeasonMidEnd", 21, 1, 100_000);

            seasonalDimensions = builder
                    .comment("Dimensions that follow the seasons. Dimensions not listed ignore seasons entirely.")
                    .defineList("seasonalDimensions", List.of("minecraft:overworld"), o -> o instanceof String);

            builder.pop();

            builder.comment("Seasonal crops: growth gating and withering driven by datapack data.")
                    .push("seasonalCrops");

            seasonalCropsEnabled = builder
                    .comment("Master switch. When false, no crop is ever gated regardless of loaded data.")
                    .define("enabled", true);

            slowFactor = builder
                    .comment("Growth-chance multiplier applied to crops with the 'slow' out-of-season rule.")
                    .defineInRange("slowFactor", 0.25, 0.0, 1.0);

            witherDropsSeed = builder
                    .comment("Whether a crop that withers drops its seed.")
                    .define("witherDropsSeed", false);

            defaultRule = builder
                    .comment("Out-of-season rule for crops that have no datapack entry and do not define their own.")
                    .defineEnum("defaultRule", net.bettytiger.bbetter_seasons.seasonal.OutOfSeasonRule.NONE);

            builder.pop();

            builder.comment("Day-change chat notifications.").push("notifications");

            chatNotifications = builder
                    .comment("Announce the day's calendar events in chat when the day changes.")
                    .define("chatNotifications", true);

            notificationLeadDays = builder
                    .comment("Also announce events up to this many days ahead (0 = today only).")
                    .defineInRange("leadDays", 3, 0, 100);

            builder.pop();

            builder.comment("Calendar event editing.").push("events");

            eventEditPermission = builder
                    .comment("Who may create/edit/delete events: OWNER_ONLY (own events; world events op-only), OP, or ALL.")
                    .defineEnum("eventEditPermission", net.bettytiger.bbetter_seasons.calendar.EditPermission.OWNER_ONLY);

            builder.pop();
        }
    }
}
