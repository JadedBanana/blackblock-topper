package rocks.blackblock.topper.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.ScoreHolderArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import rocks.blackblock.core.commands.CommandCreator;
import rocks.blackblock.core.commands.CommandLeaf;
import rocks.blackblock.core.statistics.StatFormat;
import rocks.blackblock.topper.creative.CreativeScreen;
import rocks.blackblock.topper.statistics.CustomStatistic;
import rocks.blackblock.topper.statistics.CustomStatisticPertainability;
import rocks.blackblock.topper.statistics.CustomStatisticsComponent;
import rocks.blackblock.topper.statistics.StatisticsScreen;

import java.util.*;

public class Commands {

    private static final CommandLeaf BLACKBLOCK = CommandCreator.getPermissionRoot("blackblock", "blackblock.mod");
    private static final CommandLeaf BBSTATS = CommandCreator.getRoot("bbstats");

    public static void register() {
        // Creative command
        addCreativeCommand();

        // BBStats command
        addStatisticsCommands();

    }

    /**
     * Add creative commands.
     * Usage: /blackblock creative
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.1.0
     */
    private static void addCreativeCommand() {
        CommandLeaf creative_leaf = BLACKBLOCK.getChild("creative");
        creative_leaf.onExecute(context -> {
            // Get attributes.
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayer();

            // Open screen.
            if (player == null) return 0;
            player.openHandledScreen(new CreativeScreen(player));
            return 1;
        });
    }

    /**
     * Add statistics commands.
     * Usage: /bbstats
     * Usage: /bbstats gui
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    private static void addStatisticsCommands() {
        // Set bbstats on its own to just open the gui.
        BBSTATS.onExecute(context -> {
            // Get attributes.
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayer();

            // Open screen.
            if (player == null) return 0;
            player.openHandledScreen(new StatisticsScreen(player));
            return 1;
        });

        // Also add a separate gui command so users don't get confused.
        CommandLeaf gui = BBSTATS.getChild("gui");
        gui.onExecute(context -> {
            // Get attributes.
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayer();

            // Open screen.
            if (player == null) return 0;
            player.openHandledScreen(new StatisticsScreen(player));
            return 1;
        });

        // Add commands under the larger branches.
        addStatisticsPlayersCommands();
        addStatisticsStatsCommands();
    }

    /**
     * Add statistics commands under the 'players' branch.
     * Allows for setting/adding/subtracting from players' stats.
     * These can be used by custom stat maintainers, ops, or command blocks, and take player selectors.
     * Usage: /bbstats players
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    private static void addStatisticsPlayersCommands() {
        CommandLeaf players = BBSTATS.getChild("players");

        /**
         * Player add command. Adds a given amount to a player's stat total.
         * Usage: /bbstats players add <key> <target> <amount>
         *
         * @author   Jade Godwin          <icanhasabanana@gmail.com>
         * @since    0.2.0
         */
        CommandLeaf players_add = players.getChild("add");
        CommandLeaf add_key = addCustomStatisticSelection(players_add, CustomStatisticPertainability.MAINTAINS,null);
        CommandLeaf add_target = add_key.getChild("targets");
        add_target.setType(ScoreHolderArgumentType.scoreHolders()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER);
        CommandLeaf add_amount = add_target.getChild("amount");
        add_amount.setType(IntegerArgumentType.integer());

        // Executing on the target argument defaults with an amount of 1.
        add_target.onExecute(context ->
                getCustomStatAndExecute(context, CustomStatisticPertainability.MAINTAINS, ((context1, player, statistic) -> {
                    // Gather target names.
                    List<String> target_names = getPlayerNamesFromScoreHoldersType(context, "targets");
                    if (target_names == null) return 0;

                    // Add the scores.
                    int last_score = 0;
                    for (String target : target_names)
                        last_score = statistic.addScore(target, 1);

                    // Send feedback to player and return last score in the list.
                    CustomStatisticsComponent.getInstance().markDirty();
                    if (target_names.size() == 1) {
                        context.getSource().sendFeedback(() -> Text.literal("Added 1 to " + target_names.get(0) + "'s [" + statistic.getDisplayName() + "] stat"), true);
                        return last_score;
                    }
                    context.getSource().sendFeedback(() -> Text.literal("Added 1 to [" + statistic.getDisplayName() + "] for " + target_names.size() + " players"), true);
                    return target_names.size();
                }))
        );

        // Executing on the amount argument actually gives the requested amount.
        add_amount.onExecute(context ->
                getCustomStatAndExecute(context, CustomStatisticPertainability.MAINTAINS, ((context1, player, statistic) -> {
                    // Gather target names.
                    List<String> target_names = getPlayerNamesFromScoreHoldersType(context, "targets");
                    if (target_names == null) return 0;

                    // Add the scores.
                    int amount = IntegerArgumentType.getInteger(context, "amount");
                    int last_score = 0;
                    for (String target : target_names)
                        last_score = statistic.addScore(target, amount);

                    // Send feedback to player and return last score in the list.
                    CustomStatisticsComponent.getInstance().markDirty();
                    if (target_names.size() == 1) {
                        context.getSource().sendFeedback(() -> Text.literal("Added " + amount + " to " + target_names.get(0) + "'s [" + statistic.getDisplayName() + "] stat"), true);
                        return last_score;
                    }
                    context.getSource().sendFeedback(() -> Text.literal("Added " + amount + " to [" + statistic.getDisplayName() + "] for " + target_names.size() + " players"), true);
                    return target_names.size();
                }))
        );

        /**
         * Player get command. Simply returns the given player's stat total.
         * Usage: /bbstats players get <key> <target>
         *
         * @author   Jade Godwin          <icanhasabanana@gmail.com>
         * @since    0.2.0
         */
        CommandLeaf players_get = players.getChild("get");
        CommandLeaf get_key = addCustomStatisticSelection(players_get, CustomStatisticPertainability.MAINTAINS,null);
        CommandLeaf get_target = get_key.getChild("target");
        get_target.setType(ScoreHolderArgumentType.scoreHolders()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER);
        get_target.onExecute(context ->
                getCustomStatAndExecute(context, CustomStatisticPertainability.MAINTAINS, ((context1, player, statistic) -> {
                    // Gather target name.
                    String target = getPlayerNameFromScoreHolderType(context, "target");
                    if (target == null) return 0;

                    // Send feedback to player.
                    int score = statistic.getScore(target);
                    context.getSource().sendFeedback(() -> Text.literal(target + " has a [" + statistic.getDisplayName() + "] score of " + score), false);
                    return score;
                }))
        );

        /**
         * Player list command. When used on a key, it will return a list of players' scores, ordered from top to bottom.
         * Usage: /bbstats players list <key>
         *
         * @author   Jade Godwin          <icanhasabanana@gmail.com>
         * @since    0.2.0
         */
        CommandLeaf players_list = players.getChild("list");
        CommandLeaf list_key = addCustomStatisticSelection(players_list, CustomStatisticPertainability.MAINTAINS,
            (context, player, statistic) -> {
                // Send feedback to player.
                List<Pair<String, Integer>> scores = statistic.getScores();
                scores.sort(Comparator.comparing(scorePair -> -scorePair.getRight()));
                if (scores.isEmpty()) context.getSource().sendFeedback(() -> Text.literal("[" + statistic.getDisplayName() + "] has no player scores"), false);
                else {
                    context.getSource().sendFeedback(() -> Text.literal("[" + statistic.getDisplayName() + "] has the following scores:"), false);
                    for (int i = 0; i < scores.size(); i++) {
                        Text text = Text.literal( "#" + (i + 1) + ": ").formatted(Formatting.YELLOW).append(Text.literal(scores.get(i).getLeft() + ": " + scores.get(i).getRight()).formatted(Formatting.WHITE));
                        context.getSource().sendFeedback(() -> text, false);
                    }
                }

                // Return score list size.
                return scores.size();
            });

        /**
         * Player remove command. Subtracts the given amount from the given players' stat.
         * Usage: /bbstats players remove <key> <target> <amount>
         *
         * @author   Jade Godwin          <icanhasabanana@gmail.com>
         * @since    0.2.0
         */
        CommandLeaf players_remove = players.getChild("remove");
        CommandLeaf remove_key = addCustomStatisticSelection(players_remove, CustomStatisticPertainability.MAINTAINS,null);
        CommandLeaf remove_target = remove_key.getChild("targets");
        remove_target.setType(ScoreHolderArgumentType.scoreHolders()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER);
        CommandLeaf remove_amount = remove_target.getChild("amount");
        remove_amount.setType(IntegerArgumentType.integer());

        // Executing on the target argument defaults with an amount of 1.
        remove_target.onExecute(context ->
                getCustomStatAndExecute(context, CustomStatisticPertainability.MAINTAINS, ((context1, player, statistic) -> {
                    // Gather target names.
                    List<String> target_names = getPlayerNamesFromScoreHoldersType(context, "targets");
                    if (target_names == null) return 0;

                    // Subtract the scores.
                    int last_score = 0;
                    for (String target : target_names)
                        last_score = statistic.removeScore(target, 1);

                    // Send feedback to player and return last score in the list.
                    CustomStatisticsComponent.getInstance().markDirty();
                    if (target_names.size() == 1) {
                        context.getSource().sendFeedback(() -> Text.literal("Subtracted 1 from " + target_names.get(0) + "'s [" + statistic.getDisplayName() + "] stat"), true);
                        return last_score;
                    }
                    context.getSource().sendFeedback(() -> Text.literal("Subtracted 1 from [" + statistic.getDisplayName() + "] for " + target_names.size() + " players"), true);
                    return target_names.size();
                }))
        );

        // Executing on the amount argument actually gives the requested amount.
        remove_amount.onExecute(context ->
                getCustomStatAndExecute(context, CustomStatisticPertainability.MAINTAINS, ((context1, player, statistic) -> {
                    // Gather target names.
                    List<String> target_names = getPlayerNamesFromScoreHoldersType(context, "targets");
                    if (target_names == null) return 0;

                    // Subtract the scores.
                    int amount = IntegerArgumentType.getInteger(context, "amount");
                    int last_score = 0;
                    for (String target : target_names)
                        last_score = statistic.removeScore(target, amount);

                    // Send feedback to player and return last score in the list.
                    CustomStatisticsComponent.getInstance().markDirty();
                    if (target_names.size() == 1) {
                        context.getSource().sendFeedback(() -> Text.literal("Subtracted " + amount + " from " + target_names.get(0) + "'s [" + statistic.getDisplayName() + "] stat"), true);
                        return last_score;
                    }
                    context.getSource().sendFeedback(() -> Text.literal("Subtracted " + amount + " from [" + statistic.getDisplayName() + "] for " + target_names.size() + " players"), true);
                    return target_names.size();
                }))
        );

        /**
         * Player reset command. Sets the given players' stat total back to 0.
         * Usage: /bbstats players reset <key> <target>
         *
         * @author   Jade Godwin          <icanhasabanana@gmail.com>
         * @since    0.2.0
         */
        CommandLeaf players_reset = players.getChild("reset");
        CommandLeaf reset_key = addCustomStatisticSelection(players_reset, CustomStatisticPertainability.MAINTAINS,null);
        CommandLeaf reset_target = reset_key.getChild("targets");
        reset_target.setType(ScoreHolderArgumentType.scoreHolders()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER);
        reset_target.onExecute(context ->
                getCustomStatAndExecute(context, CustomStatisticPertainability.MAINTAINS, ((context1, player, statistic) -> {
                    // Gather target names.
                    List<String> target_names = getPlayerNamesFromScoreHoldersType(context, "targets");
                    if (target_names == null) return 0;

                    // Set the scores.
                    target_names.forEach(statistic::resetScore);

                    // Send feedback to player and return last score in the list.
                    CustomStatisticsComponent.getInstance().markDirty();
                    if (target_names.size() == 1) {
                        context.getSource().sendFeedback(() -> Text.literal("Reset " + target_names.get(0) + "'s [" + statistic.getDisplayName() + "] stat to 0"), true);
                        return 1;
                    }
                    context.getSource().sendFeedback(() -> Text.literal("Reset [" + statistic.getDisplayName() + "] for " + target_names.size() + " players to 0"), true);
                    return target_names.size();
                }))
        );

        /**
         * Player set command. Sets the given players' stat total to the given amount.
         * Usage: /bbstats players set <key> <target> <amount>
         *
         * @author   Jade Godwin          <icanhasabanana@gmail.com>
         * @since    0.2.0
         */
        CommandLeaf players_set = players.getChild("set");
        CommandLeaf set_key = addCustomStatisticSelection(players_set, CustomStatisticPertainability.MAINTAINS,null);
        CommandLeaf set_target = set_key.getChild("targets");
        set_target.setType(ScoreHolderArgumentType.scoreHolders()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER);
        CommandLeaf set_amount = set_target.getChild("amount");
        set_amount.setType(IntegerArgumentType.integer());
        set_amount.onExecute(context ->
                getCustomStatAndExecute(context, CustomStatisticPertainability.MAINTAINS, ((context1, player, statistic) -> {
                    // Gather target names.
                    List<String> target_names = getPlayerNamesFromScoreHoldersType(context, "targets");
                    if (target_names == null) return 0;

                    // Set the scores.
                    int amount = IntegerArgumentType.getInteger(context, "amount");
                    int last_score = 0;
                    for (String target : target_names)
                        last_score = statistic.setScore(target, amount);

                    // Send feedback to player and return last score in the list.
                    CustomStatisticsComponent.getInstance().markDirty();
                    if (target_names.size() == 1) {
                        context.getSource().sendFeedback(() -> Text.literal("Set " + target_names.get(0) + "'s [" + statistic.getDisplayName() + "] stat to " + amount), true);
                        return last_score;
                    }
                    context.getSource().sendFeedback(() -> Text.literal("Set [" + statistic.getDisplayName() + "] for " + target_names.size() + " players to " + amount), true);
                    return target_names.size();
                }))
        );

    }

    /**
     * Add statistics commands under the 'stats' branch.
     * Allows for creation and modification of statistics.
     * Mostly only usable by custom stat owners, ops, and command blocks.
     * Usage: /bbstats stats
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    private static void addStatisticsStatsCommands() {
        CommandLeaf stats = BBSTATS.getChild("stats");

        /**
         * Stat add command. Creates a new custom stat. Can only be used by ops and command blocks.
         * Usage: /bbstats stats add <key> <displayname> <owner>
         *
         * @author   Jade Godwin          <icanhasabanana@gmail.com>
         * @since    0.2.0
         */
        CommandLeaf stats_add = stats.getChild("add");
        CommandLeaf stats_add_key = stats_add.getChild("key");
        CommandLeaf stats_add_name = stats_add_key.getChild("displayname");
        CommandLeaf stats_add_owner = stats_add_name.getChild("owner");
        stats_add_key.setType(StringArgumentType.string());
        stats_add_name.setType(StringArgumentType.string());
        stats_add_owner.setType(ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER);
        stats_add.requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(1));
        stats_add_owner.onExecute(context -> {
            // Make sure the owner_name is good.
            String owner_name = getPlayerNameFromScoreHolderType(context, "owner");
            if (owner_name == null)
                return 0;

            // Make sure key name is good.
            String key = StringArgumentType.getString(context, "key");
            if (!Identifier.isPathValid(key)) {
                context.getSource().sendFeedback(() -> Text.literal("Stat keys must only have a-z, 0-9, and underscore as characters!").formatted(Formatting.RED), false);
                return 0;
            }

            // Create stat.
            int return_value = CustomStatisticsComponent.getInstance().createCustomStatistic(
                    key, StringArgumentType.getString(context, "displayname"), owner_name);

            // Send feedback and return.
            if (return_value == 0) context.getSource().sendFeedback(() -> Text.literal("A custom statistic already exists by that name").formatted(Formatting.RED), false);
            else context.getSource().sendFeedback(() -> Text.literal("Created new custom stat [" + StringArgumentType.getString(context, "displayname") + "]"), true);
            return return_value;
        });


        /**
         * Stat remove command. Deletes a custom stat. Can only be used by ops and command blocks.
         * Usage: /bbstats stats remove <key>
         *
         * @author   Jade Godwin          <icanhasabanana@gmail.com>
         * @since    0.2.0
         */
        CommandLeaf stats_remove = stats.getChild("remove");
        stats_remove.requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(1));
        addCustomStatisticSelection(stats_remove, CustomStatisticPertainability.ALL,
                (context, player, statistic) -> {
                    // Remove the selected stat.
                    CustomStatisticsComponent.getInstance().deleteCustomStatistic(statistic);
                    context.getSource().sendFeedback(() -> Text.literal("Deleted custom stat [" + statistic.getDisplayName() + "]"), true);
                    return 1;
                });

        /**
         * Stat list command. Lists all custom stats. Can be used by anyone.
         * Usage: /bbstats stats list
         *
         * @author   Jade Godwin          <icanhasabanana@gmail.com>
         * @since    0.2.0
         */
        CommandLeaf stats_list = stats.getChild("list");
        stats_list.onExecute(context -> {
            // Get statistics list. Output.
            List<CustomStatistic> statistics = CustomStatisticsComponent.getInstance().getCustomStatistics();
            if (statistics.isEmpty())
                context.getSource().sendFeedback(() -> Text.literal("There are no custom stats"), false);
            else {
                List<String> names = new ArrayList<>();
                statistics.forEach(statistic -> names.add(statistic.getDisplayName()));
                context.getSource().sendFeedback(() -> Text.literal("There are " + statistics.size() + " custom stat(s): [" +
                        String.join("], [", names) + "]"), false);
            }
            return 1;
        });

        /**
         * Stat get command. Lists all the custom stat's attributes. Can be used by anyone.
         * Usage: /bbstats stats get <key>
         *
         * @author   Jade Godwin          <icanhasabanana@gmail.com>
         * @since    0.2.0
         */
        CommandLeaf stats_get = stats.getChild("get");
        addCustomStatisticSelection(stats_get, CustomStatisticPertainability.ALL,
            (context, player, statistic) -> {
                context.getSource().sendFeedback(() -> Text.literal("[" + statistic.getDisplayName() + "] has the following properties:"), false);
                context.getSource().sendFeedback(() -> Text.literal( "- ").formatted(Formatting.YELLOW).append(Text.literal("Owner: " + statistic.getOwner()).formatted(Formatting.WHITE)), false);
                context.getSource().sendFeedback(() -> Text.literal( "- ").formatted(Formatting.YELLOW).append(Text.literal("Maintainer(s): " + String.join(", ", statistic.getMaintainers())).formatted(Formatting.WHITE)), false);
                context.getSource().sendFeedback(() -> Text.literal( "- ").formatted(Formatting.YELLOW).append(Text.literal("Display Item: " + statistic.getDisplayItem().getName().getString()).formatted(Formatting.WHITE)), false);
                context.getSource().sendFeedback(() -> Text.literal( "- ").formatted(Formatting.YELLOW).append(Text.literal("Number format: " + statistic.getFormat().asString()).formatted(Formatting.WHITE)), false);
                return 1;
            });



        /**
         * Stat modify command. Used to modify things like display name, display item, maintainers, and owner for
         * a given custom stat. Can only be used by stat owners, ops, and command blocks.
         * Multiple different branches on this one, see usages in comments.
         *
         * @author   Jade Godwin          <icanhasabanana@gmail.com>
         * @since    0.2.0
         */
        CommandLeaf stats_modify = stats.getChild("modify");
        CommandLeaf stats_modify_key = addCustomStatisticSelection(stats_modify, CustomStatisticPertainability.OWNS, null);

        // Modify display name
        // Usage: /bbstats stats modify <key> displayname <name>
        CommandLeaf modify_displayname = stats_modify_key.getChild("displayname");
        CommandLeaf displayname_input = modify_displayname.getChild("name");
        displayname_input.setType(StringArgumentType.string());
        displayname_input.onExecute(context ->
            getCustomStatAndExecute(context, CustomStatisticPertainability.OWNS, ((context1, player, statistic) -> {
                // Set display name, then send feedback.
                int return_value = statistic.setDisplayName(StringArgumentType.getString(context, "name"));
                if (return_value == 0) context.getSource().sendFeedback(() -> Text.literal("Failed to change the display name of " + statistic.getKey()).formatted(Formatting.RED), false);
                else context.getSource().sendFeedback(() -> Text.literal("Changed the display name of " + statistic.getKey() + " to [" + statistic.getDisplayName() + "]"), true);
                CustomStatisticsComponent.getInstance().markDirty();
                return return_value;
            }))
        );

        // Modify display item
        // Usage: /bbstats stats modify <key> displayitem <set/reset>
        CommandLeaf modify_displayitem = stats_modify_key.getChild("displayitem");
        // Resetting the display item will make it default to the stat owner's head.
        CommandLeaf displayitem_reset = modify_displayitem.getChild("reset");
        displayitem_reset.onExecute(context ->
            getCustomStatAndExecute(context, CustomStatisticPertainability.OWNS, ((context1, player, statistic) -> {
                statistic.setDisplayItem(null);
                context.getSource().sendFeedback(() -> Text.literal("Reset the display item for [" + statistic.getDisplayName() + "]"), true);
                CustomStatisticsComponent.getInstance().markDirty();
                return 1;
            }))
        );
        // Setting the display item will make its icon whatever is in the player's hand at the given moment.
        CommandLeaf displayitem_set = modify_displayitem.getChild("set");
        displayitem_set.onExecute(context ->
                getCustomStatAndExecute(context, CustomStatisticPertainability.OWNS, ((context1, player, statistic) -> {
                    // Get stack in hand. Prioritize mainhand.
                    ItemStack display_item = player.getMainHandStack();
                    if (display_item.isEmpty())
                        display_item = player.getOffHandStack();

                    // If display item is empty, send error and return.
                    if (display_item.isEmpty()) {
                        context1.getSource().sendFeedback(() -> Text.literal("You need to hold an item in your hands for that!").formatted(Formatting.RED), false);
                        return 0;
                    }

                    // Set and print.
                    statistic.setDisplayItem(display_item);
                    String display_item_name = display_item.getName().getString();
                    context.getSource().sendFeedback(() -> Text.literal("Set the display item for [" + statistic.getDisplayName() + "] to [" + display_item_name + "]"), true);
                    CustomStatisticsComponent.getInstance().markDirty();
                    return 1;
                }))
        );

        // Modify format.
        // Usage: /bbstats modify <key> format <format_name>
        CommandLeaf modify_format = stats_modify_key.getChild("format");
        CommandLeaf format_name = modify_format.getChild("format_name");
        format_name.setType(StringArgumentType.string());
        format_name.suggests(StatFormat.getAllNames());
        format_name.onExecute(context ->
                getCustomStatAndExecute(context, CustomStatisticPertainability.OWNS, ((context1, player, statistic) -> {
                    // Get format provided.
                    String format_name1 = StringArgumentType.getString(context, "format_name");
                    StatFormat format = StatFormat.getByName(format_name1);

                    // Make sure a valid format was provided.
                    if (format == null) {
                        context.getSource().sendFeedback(() -> Text.literal("Invalid format!").formatted(Formatting.RED), false);
                        return 0;
                    }

                    // Set formatter for the stat.
                    statistic.setFormat(format);
                    context.getSource().sendFeedback(() -> Text.literal("Set the number format for [" + statistic.getDisplayName() + "] to " + format_name1), true);
                    return 1;
            }))
        );

        // Modify maintainers.
        // Usage: /bbstats stats modify <key> maintainers <add/remove> <maintainer_name>
        CommandLeaf modify_maintainers = stats_modify_key.getChild("maintainers");
        CommandLeaf maintainers_add = modify_maintainers.getChild("add");
        CommandLeaf add_input = maintainers_add.getChild("maintainer_to_add");
        add_input.setType(ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER);
        add_input.onExecute(context ->
            getCustomStatAndExecute(context, CustomStatisticPertainability.OWNS, ((context1, player, statistic) -> {
                // Make sure the maintainer_name is good.
                String maintainer_name = getPlayerNameFromScoreHolderType(context, "maintainer_to_add");
                if (maintainer_name == null)
                    return 0;

                // See if player already a maintainer.
                if (statistic.isMaintainer(maintainer_name)) {
                    context.getSource().sendFeedback(() -> Text.literal("Player is already a maintainer!").formatted(Formatting.RED), false);
                    return 0;
                }

                // Set maintainer and return.
                statistic.addMaintainer(maintainer_name);
                context.getSource().sendFeedback(() -> Text.literal("Added " + maintainer_name + " to the list of maintainers for custom stat [" + statistic.getDisplayName() + "]"), true);
                return 1;
            }))
        );
        CommandLeaf maintainers_remove = modify_maintainers.getChild("remove");
        CommandLeaf remove_input = maintainers_remove.getChild("maintainer_to_remove");
        remove_input.setType(ScoreHolderArgumentType.scoreHolder()).suggests((context, builder) -> {
            // Get selected custom stat and return its maintainers.
            CustomStatistic customStatistic = CustomStatisticsComponent.getInstance().getCustomStatistic(StringArgumentType.getString(context, "key"));
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (customStatistic != null && player != null) customStatistic.getMaintainers().forEach(builder::suggest);
            return builder.buildFuture();
        });
        remove_input.onExecute(context ->
                getCustomStatAndExecute(context, CustomStatisticPertainability.OWNS, ((context1, player, statistic) -> {
                    // Make sure the maintainer_name is good.
                    String maintainer_name = getPlayerNameFromScoreHolderType(context, "maintainer_to_remove");
                    if (maintainer_name == null)
                        return 0;

                    // See if player is not a maintainer.
                    if (!statistic.isMaintainer(maintainer_name)) {
                        context.getSource().sendFeedback(() -> Text.literal(maintainer_name + " is not a maintainer for [" + statistic.getDisplayName() + "]").formatted(Formatting.RED), false);
                        return 0;
                    }

                    // Return if player is the owner.
                    if (maintainer_name.equals(statistic.getOwner())) {
                        context.getSource().sendFeedback(() -> Text.literal("Cannot remove the custom stat owner from the list of maintainers!").formatted(Formatting.RED), false);
                        return 0;
                    }

                    // Otherwise, do the deletion.
                    statistic.removeMaintainer(maintainer_name);
                    context.getSource().sendFeedback(() -> Text.literal("Removed " + maintainer_name + " from the list of maintainers for custom stat [" + statistic.getDisplayName() + "]"), true);
                    return 1;
                }))
        );

        // Modify owner
        // Usage: /bbstats modify <key> owner <owner_name>
        CommandLeaf modify_owner = stats_modify_key.getChild("owner");
        CommandLeaf owner_input = modify_owner.getChild("owner_name");
        owner_input.setType(ScoreHolderArgumentType.scoreHolder()).suggests(ScoreHolderArgumentType.SUGGESTION_PROVIDER);
        owner_input.onExecute(context ->
                getCustomStatAndExecute(context, CustomStatisticPertainability.OWNS, ((context1, player, statistic) -> {
                    // Make sure the maintainer_name is good.
                    String owner_name = getPlayerNameFromScoreHolderType(context, "owner_name");
                    if (owner_name == null)
                        return 0;

                    // See if supplied owner already owns.
                    if (owner_name.equals(statistic.getOwner())) {
                        context.getSource().sendFeedback(() -> Text.literal("Player is already the owner!").formatted(Formatting.RED), false);
                        return 0;
                    }

                    // Set maintainer and return.
                    statistic.setOwner(owner_name);
                    context.getSource().sendFeedback(() -> Text.literal("Changed owner of custom stat [" + statistic.getDisplayName() + "] to " + owner_name), true);
                    return 1;
                }))
        );
    }

    /**
     * Get the custom stat referenced in the context and execute the given command.
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    private static int getCustomStatAndExecute(CommandContext<ServerCommandSource> context,
                                        CustomStatisticPertainability pertainability, CustomStatisticAction action) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        // Get custom statistic object.
        String custom_statistic_key = StringArgumentType.getString(context, "key");
        CustomStatistic customStatistic = CustomStatisticsComponent.getInstance().getCustomStatistic(StringArgumentType.getString(context, "key"));
        if (customStatistic == null) {
            source.sendFeedback(() -> Text.literal("Unknown custom stat '" + custom_statistic_key + "'").formatted(Formatting.RED), false);
            return 0;
        }

        // See if player has permission to see this object.
        if (source.hasPermissionLevel(1) || (player != null && customStatistic.pertains(player.getName().getString(), pertainability)))
            return action.executeWithStatistic(context, player, customStatistic);

        // No permissions. Send message and return.
        switch (pertainability) {
            case OWNS -> {
                source.sendFeedback(() -> Text.literal("Only the owner of a custom stat can do that.").formatted(Formatting.RED), false);
                source.sendFeedback(() -> Text.literal("This custom stat is owned by " + customStatistic.getOwner() + ".").formatted(Formatting.RED), false);
            }
            case MAINTAINS -> source.sendFeedback(() -> Text.literal("Only maintainers of a custom stat can do that.").formatted(Formatting.RED), false);
            default -> source.sendFeedback(() -> Text.literal("An unexpected error appeared. Please for the love of God, never let anyone have this error.").formatted(Formatting.RED), false);
        }
        return 0;
    }


    /**
     * Get the player name from the Singular Score Holder argument type.
     * Score Holder argument types are convenient from a user standpoint but annoying from a parsing standpoint.
     * This method should be used to make it easier.
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public static String getPlayerNameFromScoreHolderType(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        // Get scoreholder name.
        String scoreholder_name = ScoreHolderArgumentType.getScoreHolder(context, name).getNameForScoreboard();

        // Input cleansing.
        if (scoreholder_name == null || scoreholder_name.isEmpty() || !PlayerEntity.isUsernameValid(scoreholder_name)) {
            context.getSource().sendFeedback(() -> Text.literal("Not a valid player!").formatted(Formatting.RED), false);
            return null;
        }

        // Return.
        return scoreholder_name;
    }


    /**
     * Get the player name from the Multiple Score Holders argument type.
     * Score Holder argument types are convenient from a user standpoint but annoying from a parsing standpoint.
     * This method should be used to make it easier.
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public static List<String> getPlayerNamesFromScoreHoldersType(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        // Gather scoreholder names into list.
        Collection<ScoreHolder> scoreHolders = ScoreHolderArgumentType.getScoreHolders(context, name);

        // Input cleansing.
        List<String> names = new ArrayList<>();
        for (ScoreHolder scoreHolder : scoreHolders) {
            String scoreholder_name = scoreHolder.getNameForScoreboard();
            if (scoreholder_name == null || scoreholder_name.isEmpty() || !PlayerEntity.isUsernameValid(scoreholder_name)) {
                context.getSource().sendFeedback(() -> Text.literal(scoreholder_name + " is not a valid player!").formatted(Formatting.RED), false);
                return null;
            }
            names.add(scoreholder_name);
        }

        // Return.
        return names;
    }

    /**
     * Add custom statistics selection.
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    private static CommandLeaf addCustomStatisticSelection(CommandLeaf parent, CustomStatisticPertainability pertainability,
                                                           CustomStatisticAction action) {
        // Create command leaf.
        CommandLeaf key = parent.getChild("key");
        key.setType(StringArgumentType.string());

        // Set suggestions.
        key.suggests((context, builder) -> {
            List<CustomStatistic> customStatistics =
                    CustomStatisticsComponent.getInstance().getCustomStatistics(context.getSource(), pertainability);
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player != null)
                customStatistics.forEach(customStatistic -> builder.suggest(customStatistic.getKey().getPath()));
            return builder.buildFuture();
        });

        // Set execution behavior.
        if (action != null)
            key.onExecute(context -> getCustomStatAndExecute(context, pertainability, action));

        // Return.
        return key;
    }

    /**
     * The functional interface that allows an action to be passed along into a custom statistic selector.
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    @FunctionalInterface
    public interface CustomStatisticAction {
        int executeWithStatistic(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, CustomStatistic statistic) throws CommandSyntaxException;
    }
}
