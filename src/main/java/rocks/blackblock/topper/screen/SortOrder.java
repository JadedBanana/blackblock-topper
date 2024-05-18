package rocks.blackblock.topper.screen;

import net.minecraft.util.StringIdentifiable;
import rocks.blackblock.screenbuilder.BBSB;
import rocks.blackblock.screenbuilder.textures.IconTexture;

import java.util.Arrays;

public enum SortOrder implements StringIdentifiable {

    ASCENDING("Ascending", BBSB.SORT_ASCENDING),
    DESCENDING("Descending", BBSB.SORT_DESCENDING);

    // Store all values in order
    public static final SortOrder[] values = new SortOrder[]{
      ASCENDING, DESCENDING
    };

    private final String name;
    private final IconTexture icon;

    private SortOrder(String name, IconTexture icon) { this.name = name; this.icon = icon; }
    public String toString() { return this.asString(); }
    public String asString() { return this.name; }
    public IconTexture getIcon() { return this.icon; }
    public SortOrder next() { return values[(Arrays.asList(values).indexOf(this) + 1) % values.length]; }

}
