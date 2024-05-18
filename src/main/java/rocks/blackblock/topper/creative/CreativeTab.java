package rocks.blackblock.topper.creative;

import net.minecraft.util.StringIdentifiable;
import rocks.blackblock.screenbuilder.BBSB;
import rocks.blackblock.screenbuilder.textures.IconTexture;

public enum CreativeTab implements StringIdentifiable {

    BUILDING_BLOCKS("Building Blocks", BBSB.CUBE_ICON),
    FUNCTIONAL_BLOCKS("Functional Blocks", BBSB.CUBE_SPECIAL_ICON),
    MOBHEADS("Mob Heads", BBSB.VILLAGER_ICON),
    COSMETICS("Cosmetics", BBSB.TOPHAT_ICON),
    FOOD("Food", BBSB.DRUMSTICK_ICON),
    MINIGAME_ITEMS("Minigame/Event Items", BBSB.MINI_FLAG_ICON),
    OTHER_ITEMS("Other Items", BBSB.INGOT_ICON),
    ALL("All", BBSB.ASTERISK_ICON);

    private final String name;
    private final IconTexture icon;

    private CreativeTab(String name, IconTexture icon) { this.name = name; this.icon = icon; }
    public String toString() { return this.asString(); }
    public String asString() { return this.name; }
    public IconTexture getIcon() { return this.icon; }

}
