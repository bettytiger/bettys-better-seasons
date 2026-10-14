package net.bettytiger.bbetter_seasons.config;

import net.bettytiger.bbetter_seasons.client.HudCorner;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/** Client-only display config: the date HUD, seasonal tooltips, and the transition screen. */
public class BetterSeasonsClientConfig {

    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<Client, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = pair.getLeft();
        CLIENT_SPEC = pair.getRight();
    }

    public static class Client {
        public final ForgeConfigSpec.BooleanValue hudEnabled;
        public final ForgeConfigSpec.EnumValue<HudCorner> hudCorner;
        public final ForgeConfigSpec.DoubleValue hudScale;
        public final ForgeConfigSpec.IntValue hudMargin;
        public final ForgeConfigSpec.BooleanValue seasonalTooltips;
        public final ForgeConfigSpec.BooleanValue transitionScreen;
        public final ForgeConfigSpec.IntValue transitionDurationTicks;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Date HUD widget.").push("hud");

            hudEnabled = builder
                    .comment("Show the date HUD. A consumer mod may turn this off and draw its own.")
                    .define("enabled", true);
            hudCorner = builder
                    .comment("Which screen corner the HUD anchors to.")
                    .defineEnum("corner", HudCorner.TOP_LEFT);
            hudScale = builder
                    .comment("Text scale of the HUD.")
                    .defineInRange("scale", 1.0, 0.25, 4.0);
            hudMargin = builder
                    .comment("Pixel margin from the screen edge.")
                    .defineInRange("margin", 4, 0, 256);

            builder.pop();

            seasonalTooltips = builder
                    .comment("Show \"Grows in\" season lines on seed and crop tooltips.")
                    .define("seasonalTooltips", true);

            builder.comment("Season transition screen (black fade on season change).").push("transition");
            transitionScreen = builder
                    .comment("Play the fade-to-black transition when the season changes.")
                    .define("enabled", true);
            transitionDurationTicks = builder
                    .comment("Total transition length in ticks (20 ticks = 1 second).")
                    .defineInRange("durationTicks", 80, 20, 400);
            builder.pop();
        }
    }
}
