package net.bettytiger.bbetter_seasons.config;

import net.bettytiger.bbetter_seasons.client.HudCorner;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/** Client-only display config: the date HUD, F3 lines, and seasonal tooltips. */
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
        public final ForgeConfigSpec.BooleanValue f3Lines;
        public final ForgeConfigSpec.BooleanValue seasonalTooltips;

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

            f3Lines = builder
                    .comment("Show calendar lines on the F3 debug screen.")
                    .define("f3Lines", true);

            seasonalTooltips = builder
                    .comment("Show \"Grows in\" season lines on seed and crop tooltips.")
                    .define("seasonalTooltips", true);
        }
    }
}
