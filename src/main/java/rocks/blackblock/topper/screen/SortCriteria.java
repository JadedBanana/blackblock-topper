package rocks.blackblock.topper.screen;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.util.StringIdentifiable;
import rocks.blackblock.screenbuilder.BBSB;
import rocks.blackblock.screenbuilder.textures.IconTexture;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public enum SortCriteria implements StringIdentifiable {

    DEFAULT("Default", null, null),
    ALPHABETICAL("Alphabetical", BBSB.SORT_ALPHABETICAL, null),
    MINED("Times Mined", BBSB.SORT_MINED, null),
    BROKEN("Times Broken", BBSB.SORT_BROKEN, Stats.BROKEN),
    CRAFTED("Times Crafted", BBSB.SORT_CRAFTED, Stats.CRAFTED),
    USED("Times Used", BBSB.SORT_USED, Stats.USED),
    PICKED_UP("Picked Up", BBSB.SORT_PICKED_UP, Stats.PICKED_UP),
    DROPPED("Dropped", BBSB.SORT_DROPPED, Stats.DROPPED);

    // Store all values in order
    public static final SortCriteria[] values = new SortCriteria[]{
        DEFAULT, ALPHABETICAL, MINED, BROKEN, CRAFTED, USED, PICKED_UP, DROPPED
    };

    private final String name;
    private final IconTexture icon;
    private final StatType<Item> statType;

    private SortCriteria(String name, IconTexture icon, StatType<Item> statType) {
        this.name = name; this.icon = icon; this.statType = statType;
    }
    public String toString() { return this.asString(); }
    public String asString() { return this.name; }
    public IconTexture getIcon() { return this.icon; }

    public SortCriteria next() { return values[(Arrays.asList(values).indexOf(this) + 1) % values.length]; }

    public void sort(List<Item> items, SortOrder sortOrder, ServerPlayerEntity player) {
        // Alphabet sort
        if (this == SortCriteria.ALPHABETICAL) {
            items.sort(Comparator.comparing(item -> item.getName().getString()));

        // Mined stat, being the only block stat, gets its own special part.
        } else if (this == SortCriteria.MINED) {
            items.sort(Comparator.comparing(item ->
                item instanceof BlockItem blockItem ? player.getStatHandler().getStat(Stats.MINED, blockItem.getBlock()) : -1
            ));
            if (sortOrder == SortOrder.DESCENDING)
                Collections.reverse(items);
            return;

        // For all other stats, attempt to sort by the stat.
        } else if (this.statType != null) {
            items.sort(Comparator.comparing(item -> player.getStatHandler().getStat(this.statType, item)));
            if (sortOrder == SortOrder.DESCENDING)
                Collections.reverse(items);
            return;
        }

        // Final reversal for those who don't have alternate reversals.
        if (sortOrder == SortOrder.ASCENDING)
            Collections.reverse(items);
    }
}
