package rocks.blackblock.topper.statistics;

import rocks.blackblock.screenbuilder.BBSB;
import rocks.blackblock.screenbuilder.textures.IconTexture;
import rocks.blackblock.topper.screen.SortCriteria;

import java.util.Arrays;
import java.util.Collection;

public enum StatisticsTab {

    GENERAL("General Stats", BBSB.HEART_ICON, SortCriteria.DEFAULT, SortCriteria.ALPHABETICAL, SortCriteria.OWNER),
    ITEMS("Items", BBSB.INGOT_ICON, SortCriteria.DEFAULT, SortCriteria.ALPHABETICAL, SortCriteria.MINED, SortCriteria.BROKEN, SortCriteria.CRAFTED, SortCriteria.USED, SortCriteria.PICKED_UP, SortCriteria.DROPPED);

    private final String name;
    private final IconTexture icon;
    private final Collection<SortCriteria> allowed_sort_criteria;

    private StatisticsTab(String name, IconTexture icon, SortCriteria... allowed_sort_criteria) {
        this.name = name; this.icon = icon;
        this.allowed_sort_criteria = Arrays.stream(allowed_sort_criteria).toList();
    }

    public String toString() { return this.asString(); }
    public String asString() { return this.name; }
    public IconTexture getIcon() { return this.icon; }
    public Collection<SortCriteria> getAllowedSortCriteria() { return this.allowed_sort_criteria; }
}
