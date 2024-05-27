package rocks.blackblock.topper.statistics;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.nbt.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;
import rocks.blackblock.core.statistics.StatFormat;
import rocks.blackblock.topper.BlackBlockTopper;

import java.util.*;

public class CustomStatistic {

    protected String owner_name;
    protected String display_name;
    protected Identifier key;
    protected ItemStack display_item;
    protected List<String> maintainers = new ArrayList<>();
    protected HashMap<String, Integer> scores = new HashMap<>();
    protected StatFormat format = StatFormat.DEFAULT;

    protected CustomStatistic(@NotNull Identifier key, @NotNull String name, @NotNull String owner_name) {
        this.key = key;
        this.display_name = name;
        this.owner_name = owner_name;
        this.maintainers.add(owner_name);
    }

    /**
     * Getters for stat properties (owner, key, display name, maintainers, display item).
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public String getOwner() { return owner_name; }
    public Identifier getKey() { return this.key; }
    public String getDisplayName() { return this.display_name; }
    public List<String> getMaintainers() { return this.maintainers; }
    public ItemStack getDisplayItem() {
        // If the display item is not set, then return the owner's player head.
        if (this.display_item == null || this.display_item.isEmpty()) {
            ItemStack default_head = new ItemStack(Items.PLAYER_HEAD);
            default_head.setSubNbt(PlayerHeadItem.SKULL_OWNER_KEY, NbtString.of(this.getOwner()));
            return default_head;
        }
        // Return.
        return this.display_item;
    }
    public StatFormat getFormat() { return this.format; }


    /**
     * Setters for stat properties (owner, display name, maintainers, formatter, display item).
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public int setOwner(String target) {
        if (target == null || target.isEmpty() || owner_name.equals(target)) return 0;
        owner_name = target;
        return 1;
    }

    public int setDisplayName(String name) {
        if (name == null || name.isEmpty()) return 0;
        this.display_name = name;
        return 1;
    }

    public int setFormat(StatFormat format) {
        this.format = format;
        return 1;
    }

    public int setDisplayItem(ItemStack stack) {
        // If stack is null, just set it.
        if (stack == null) {
            this.display_item = stack;
        }

        // Clean up stack data a little.
        else {
            ItemStack display_stack = stack.copy();
            display_stack.setCount(1);

            // Set and return.
            this.display_item = display_stack;
        }
        return 1;
    }

    public int addMaintainer(String target) {
        if (target == null || target.isEmpty() || isMaintainer(target)) return 0;
        maintainers.add(target);
        return 1;
    }

    public int removeMaintainer(String target) {
        if (target == null || target.isEmpty() || !isMaintainer(target) || owner_name.equals(target)) return 0;
        maintainers.remove(target);
        return 1;
    }

    /**
     * Getters & setters for all things related to actually setting players' scores.
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public int getScore(String target) {
        if (this.scores.containsKey(target))
            return this.scores.get(target);
        return 0;
    }

    public String getFormattedScore(String target) { return this.format.getFormatter().format(this.getScore(target)); }

    public List<Pair<String, Integer>> getScores() {
        // Put scores on a list and return.
        List<Pair<String, Integer>> scores = new ArrayList<>();
        this.scores.forEach((name, score) -> scores.add(new Pair<>(name, score)));
        return scores;
    }

    public int resetScore(String target) { return this.setScore(target, 0); }
    public int setScore(String target, Integer value) {
        if (target == null || target.isEmpty()) return 0;
        this.scores.put(target, value);
        return getScore(target);
    }

    public int removeScore(String target, int value) { return addScore(target, -value); }
    public int addScore(String target, int value) {
        if (target == null || target.isEmpty()) return 0;
        this.scores.put(target, this.getScore(target) + value);
        return getScore(target);
    }

    /**
     * Is the given user a maintainer of this stat?
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public boolean isMaintainer(String target) { return maintainers.contains(target); }

    /**
     * If the given user pertains to this statistic in the given way.
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public boolean pertains(String target, CustomStatisticPertainability pertainability) {
        return switch(pertainability) {
            case ALL -> true;
            case OWNS -> owner_name.equals(target);
            case MAINTAINS -> isMaintainer(target);
        };
    }

    /**
     * Generate stat from NBT.
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public static CustomStatistic fromNbt(NbtElement nbt) {
        // Only load if all the data is present.
        if (nbt instanceof NbtCompound compound &&
                compound.contains("owner_name", NbtElement.STRING_TYPE) &&
                compound.contains("display_name", NbtElement.STRING_TYPE) &&
                compound.contains("key", NbtElement.STRING_TYPE)) {

            // Try and convert string key to identifier key.
            Identifier key = Identifier.tryParse(compound.getString("key"));
            if (key == null) {
                BlackBlockTopper.LOGGER.error("Failed to load a custom statistic " + compound.getString("key") + "!");
                return null;
            }

            // Instantiate custom statistic object.
            CustomStatistic customStatistic = new CustomStatistic(
                    key, compound.getString("display_name"), compound.getString("owner_name"));

            // Pull maintainers, if exists.
            if (compound.contains("maintainers", NbtElement.LIST_TYPE)) {
                NbtList maintainers_list = compound.getList("maintainers", NbtElement.STRING_TYPE);
                maintainers_list.forEach(nbtElement -> {
                    customStatistic.addMaintainer(nbtElement.asString());
                });
            }

            // Pull scores, if exists.
            if (compound.contains("scores", NbtElement.COMPOUND_TYPE)) {
                NbtCompound scores = compound.getCompound("scores");
                scores.getKeys().forEach(username -> {
                    customStatistic.setScore(username, scores.getInt(username));
                });
            }

            // Pull format, if exists.
            if (compound.contains("format", NbtElement.STRING_TYPE)) {
                StatFormat format = StatFormat.getByName(compound.getString("format"));
                if (format != null)
                    customStatistic.setFormat(format);
            }

            // Return.
            return customStatistic;
        }

        // Return null if there's not enough data there.
        BlackBlockTopper.LOGGER.error("Failed to load a custom statistic!");
        return null;
    }

    /**
     * Write stat to NBT.
     *
     * @author   Jade Godwin          <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    public NbtCompound toNbt() {
        // Put maintainers and scores in nbt structures.
        NbtList maintainers = new NbtList();
        this.maintainers.forEach(name -> { maintainers.add(NbtString.of(name)); });
        NbtCompound scores = new NbtCompound();
        this.scores.forEach(scores::putInt);

        // Put everything else.
        NbtCompound stat_info = new NbtCompound();
        stat_info.putString("key", this.key.toString());
        stat_info.putString("display_name", this.display_name);
        stat_info.putString("owner_name", this.owner_name);
        stat_info.putString("format", this.format.asString());
        stat_info.put("maintainers", maintainers);
        stat_info.put("scores", scores);

        // Return.
        return stat_info;
    }
}
