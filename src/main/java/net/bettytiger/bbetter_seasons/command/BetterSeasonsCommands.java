package net.bettytiger.bbetter_seasons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.bettytiger.bbetter_seasons.BettysBetterSeasonsMod;
import net.bettytiger.bbetter_seasons.calendar.CalendarDate;
import net.bettytiger.bbetter_seasons.calendar.CalendarManager;
import net.bettytiger.bbetter_seasons.calendar.Season;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

/** {@code /calendar} and {@code /season}. Query is open to everyone; set requires permission level 2. */
public class BetterSeasonsCommands {

    private static final SimpleCommandExceptionType NO_CALENDAR =
            new SimpleCommandExceptionType(Component.translatable("commands." + BettysBetterSeasonsMod.MODID + ".unavailable"));

    private static final SuggestionProvider<CommandSourceStack> SEASON_SUGGESTIONS = (ctx, builder) -> {
        CalendarManager mgr = CalendarManager.get();
        if (mgr != null) {
            for (Season season : mgr.seasons()) {
                builder.suggest(season.id().toString());
                builder.suggest(season.id().getPath());
            }
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(calendar());
        dispatcher.register(season());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> calendar() {
        return Commands.literal("calendar")
                .executes(BetterSeasonsCommands::query)
                .then(Commands.literal("query").executes(BetterSeasonsCommands::query))
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("year", IntegerArgumentType.integer())
                                .then(Commands.argument("season", StringArgumentType.word())
                                        .suggests(SEASON_SUGGESTIONS)
                                        .then(Commands.argument("day", IntegerArgumentType.integer(1))
                                                .executes(BetterSeasonsCommands::setDate)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> season() {
        return Commands.literal("season")
                .executes(BetterSeasonsCommands::query)
                .then(Commands.literal("query").executes(BetterSeasonsCommands::query))
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("season", StringArgumentType.word())
                                .suggests(SEASON_SUGGESTIONS)
                                .executes(BetterSeasonsCommands::setSeason)));
    }

    private static CalendarManager manager() throws CommandSyntaxException {
        CalendarManager mgr = CalendarManager.get();
        if (mgr == null) {
            throw NO_CALENDAR.create();
        }
        return mgr;
    }

    private static int query(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CalendarManager mgr = manager();
        CalendarDate date = mgr.currentDate();
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands." + BettysBetterSeasonsMod.MODID + ".calendar.query", date.describe(mgr.seasons())),
                false);
        return 1;
    }

    private static int setDate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CalendarManager mgr = manager();
        int year = IntegerArgumentType.getInteger(ctx, "year");
        int seasonIndex = resolveSeason(mgr, StringArgumentType.getString(ctx, "season"));
        int day = IntegerArgumentType.getInteger(ctx, "day");
        int maxDay = mgr.daysPerSeason();
        if (day > maxDay) {
            throw new SimpleCommandExceptionType(
                    Component.translatable("commands." + BettysBetterSeasonsMod.MODID + ".day_out_of_range", maxDay)).create();
        }
        mgr.setAbsDay(mgr.toAbsDay(year, seasonIndex, day));
        return announceSet(ctx, mgr);
    }

    private static int setSeason(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CalendarManager mgr = manager();
        int seasonIndex = resolveSeason(mgr, StringArgumentType.getString(ctx, "season"));
        CalendarDate current = mgr.currentDate();
        mgr.setAbsDay(mgr.toAbsDay(current.year(), seasonIndex, current.day()));
        return announceSet(ctx, mgr);
    }

    private static int announceSet(CommandContext<CommandSourceStack> ctx, CalendarManager mgr) {
        CalendarDate date = mgr.currentDate();
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands." + BettysBetterSeasonsMod.MODID + ".set", date.describe(mgr.seasons())),
                true);
        return 1;
    }

    /** Matches a season by full id ({@code ns:path}) or by path alone, case-insensitively. */
    private static int resolveSeason(CalendarManager mgr, String input) throws CommandSyntaxException {
        List<Season> seasons = mgr.seasons();
        for (int i = 0; i < seasons.size(); i++) {
            Season season = seasons.get(i);
            if (season.id().toString().equalsIgnoreCase(input) || season.id().getPath().equalsIgnoreCase(input)) {
                return i;
            }
        }
        throw new SimpleCommandExceptionType(
                Component.translatable("commands." + BettysBetterSeasonsMod.MODID + ".unknown_season", input)).create();
    }
}
