package rocks.blackblock.topper.statistics;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.ScoreHolderArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.StatFormatter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import rocks.blackblock.core.BlackBlockCore;
import rocks.blackblock.core.component.Component;
import rocks.blackblock.core.helper.PlayerHelper;
import rocks.blackblock.core.utils.BBLog;
import rocks.blackblock.topper.BlackBlockTopper;
import rocks.blackblock.topper.server.Commands;

import java.util.ArrayList;
import java.util.List;

public class CustomStatisticsComponent implements Component.Global {

    private static CustomStatisticsComponent INSTANCE = null;
    private final List<CustomStatistic> customStatisticList = new ArrayList<>();
    private boolean is_dirty = false;

    public CustomStatisticsComponent() {
        if (INSTANCE != null) {
            BBLog.log("CustomStatisticsComponent already exists!");
        } else {
            BBLog.attention("Creating CustomStatisticsComponent!");
            INSTANCE = this;
        }
    }

    @Override
    public boolean isDirty() { return this.is_dirty; }

    @Override
    public void setDirty(boolean dirty) { this.is_dirty = dirty; }

    /**
     * Revive the custom blackblock stats from the given NBT data
     *
     * @author   Jade Godwin        <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    @Override
    public void readFromNbt(NbtCompound tag) {
        // Get custom statistics list.
        NbtList list = tag.getList("custom_statistics", NbtElement.COMPOUND_TYPE);

        // For each entry in the list, add a new custom statistic.
        list.forEach(nbtElement -> {
            CustomStatistic customStatistic = CustomStatistic.fromNbt(nbtElement);
            if (customStatistic != null)
                customStatisticList.add(customStatistic);
        });
    }

    /**
     * Write the custom blackblock stats to the given NBT data
     *
     * @author   Jade Godwin        <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    @Override
    public NbtCompound writeToNbt(NbtCompound tag) {
        // Create an NBT List and add each statistic compound to it.
        NbtList list = new NbtList();
        customStatisticList.forEach(customStatistic -> list.add(customStatistic.toNbt()));
        tag.put("custom_statistics", list);
        return tag;
    }

    /**
     * Get a custom statistic out of the list via key.
     *
     * @author   Jade Godwin        <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public CustomStatistic getCustomStatistic(String key) {
        Identifier key_identifier = Identifier.of("bbstats", key);
        for (CustomStatistic statistic : customStatisticList)
            if (statistic.getKey().equals(key_identifier)) return statistic;
        return null;
    }

    /**
     * Get all custom statistics.
     *
     * @author   Jade Godwin        <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public List<CustomStatistic> getCustomStatistics() {
        // Prepare returned list.
        List<CustomStatistic> returned_list = new ArrayList<>();
        returned_list.addAll(customStatisticList);
        return returned_list;
    }

    /**
     * Get the custom statistics that pertain to the given player.
     *
     * @author   Jade Godwin        <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public List<CustomStatistic> getCustomStatistics(ServerCommandSource serverCommandSource, CustomStatisticPertainability pertainability) {
        // If pertainability is all, just put them all on there.
        if (pertainability == CustomStatisticPertainability.ALL || serverCommandSource.hasPermissionLevel(1))
            return getCustomStatistics();

        // Prepare returned list and get server player.
        List<CustomStatistic> returned_list = new ArrayList<>();
        ServerPlayerEntity player = serverCommandSource.getPlayer();
        if (player != null) {
            String player_name = player.getName().getString();

            // If pertainability is owned, get all that have matching UUIDs in ownership.
            if (pertainability == CustomStatisticPertainability.OWNS) {
                customStatisticList.forEach(customStatistic -> {
                    if (customStatistic.getOwner().equals(player_name))
                        returned_list.add(customStatistic);
                });
            }

            // If pertainability is maintains, get all that have matching UUIDs in maintainers.
            else if (pertainability == CustomStatisticPertainability.MAINTAINS) {
                customStatisticList.forEach(customStatistic -> {
                    if (customStatistic.isMaintainer(player_name))
                        returned_list.add(customStatistic);
                });
            }
        }

        // Return.
        return returned_list;
    }

    /**
     * Create a new custom statistic
     *
     * @author   Jade Godwin        <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public int createCustomStatistic(String key, String name, String owner_name) {
        // If a custom statistic already exists by the given key, just return.
        if (this.getCustomStatistic(key) != null)
            return 0;

        // Create and add a new CustomStatistic.
        CustomStatistic new_stat = new CustomStatistic(Identifier.of("bbstats", key), name, owner_name);
        customStatisticList.add(new_stat);
        this.markDirty();
        return 1;
    }

    /**
     * Delete a custom statistic.
     *
     * @author   Jade Godwin        <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public int deleteCustomStatistic(CustomStatistic statistic) {
        // Remove.
        customStatisticList.remove(statistic);
        this.markDirty();
        return 1;
    }

    /**
     * Get the CustomStatisticsComponent instance
     *
     * @author   Jade Godwin        <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public static CustomStatisticsComponent getInstance() {
        if (INSTANCE != null) return INSTANCE;
        return BlackBlockTopper.CUSTOM_STATS.get();
    }
}
