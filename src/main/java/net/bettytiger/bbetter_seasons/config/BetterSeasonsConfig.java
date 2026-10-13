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
        }
    }
}
