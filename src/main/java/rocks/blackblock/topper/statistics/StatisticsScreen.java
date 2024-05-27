package rocks.blackblock.topper.statistics;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import rocks.blackblock.core.BlackBlockCore;
import rocks.blackblock.screenbuilder.BBSB;
import rocks.blackblock.screenbuilder.ScreenBuilder;
import rocks.blackblock.screenbuilder.interfaces.SlotEventListener;
import rocks.blackblock.screenbuilder.slots.ButtonWidgetSlot;
import rocks.blackblock.topper.BlackBlockTopper;
import rocks.blackblock.topper.screen.ItemBrowsingScreen;
import rocks.blackblock.topper.screen.SortCriteria;
import rocks.blackblock.topper.screen.SortOrder;

import java.util.*;

/**
 * The Blackblock Statistics screen.
 * Displays Blackblock-specific statistics.
 *
 * @author   Jade Godwin          <icanhasabanana@gmail.com>
 * @since    0.2.0
 */
public class StatisticsScreen extends ItemBrowsingScreen {

    private StatisticsTab selected_tab = StatisticsTab.GENERAL;
    private SortCriteria sort_criteria = SortCriteria.DEFAULT;
    private SortOrder sort_order = SortOrder.DESCENDING;

    public StatisticsScreen(ServerPlayerEntity player) {
        super();
        this.player = player;
    }

    /**
     * Get all the custom items & blocks
     * Ordered by the current sort order and filtered by the current tab.
     *
     * @author  Jade Godwin         <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    private List<Item> getItems() {
        // If all items does not yet exist, make it.
        if (BlackBlockTopper.CREATIVE_ITEMS_FLATTENED == null)
            BlackBlockTopper.CREATIVE_ITEMS_FLATTENED = BlackBlockTopper.CREATIVE_ITEMS.values().stream().toList();

        // Copy all items over.
        ArrayList<Item> returned_items = new ArrayList<>();
        returned_items.addAll(BlackBlockTopper.CREATIVE_ITEMS_FLATTENED);

        // Implement sort criteria & return.
        this.sort_criteria.sort(returned_items, this.sort_order, this.player);
        return returned_items;
    }

    /**
     * Get all the general stats under the blackblock and bbstats namespaces.
     * Because of the way the general stats work, these have to be re-generated every time as they are NOT shared
     * between players.
     *
     * @author  Jade Godwin         <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    private List<ItemStack> getGeneralStats() {
        // Create item stacks from mod-level statistics.
        List<ItemStack> mod_stacks = new ArrayList<>();
        BlackBlockTopper.STAT_ITEMS.forEach((id, item) -> {
            // Instantiate item stack.
            ItemStack stack = new ItemStack(item);

            // Give the stack the appropriate name (translatable) and value, then put into list.
            stack.setCustomName(Text.translatable("stat." + id.toTranslationKey()).append(Text.literal(": ").append(Text.literal(String.valueOf(player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(id, BlackBlockCore.STAT_FORMATS.get(id).getFormatter())))).formatted(Formatting.WHITE))).setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withItalic(false)));
            mod_stacks.add(stack);
        });

        // If sort criteria is default, do an alphabetical sort NOW.
        if (this.sort_criteria == SortCriteria.DEFAULT) SortCriteria.ALPHABETICAL.sort(mod_stacks, player, SortOrder.DESCENDING);

        // Create item stacks from mod-level statistics.
        List<ItemStack> custom_stacks = new ArrayList<>();
        CustomStatisticsComponent.getInstance().getCustomStatistics().forEach(customStatistic -> {
            // Instantiate item stack.
            ItemStack stack = customStatistic.getDisplayItem().copy();

            // Give the stack the appropriate name and value, put the owner in the lore and NBT, then put into list.
            stack.setCustomName(Text.literal(customStatistic.getDisplayName()).append(Text.literal(": ").append(Text.literal(String.valueOf(customStatistic.getFormattedScore(player.getName().getString()))).formatted(Formatting.WHITE))).setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withItalic(false)));
            stack.setSubNbt("custom_stat_owner", NbtString.of(customStatistic.getOwner()));
            custom_stacks.add(stack);
        });

        // If sort criteria is default, do an alphabetical THEN an owner sort NOW.
        if (this.sort_criteria == SortCriteria.DEFAULT) {
            SortCriteria.ALPHABETICAL.sort(custom_stacks, player, SortOrder.DESCENDING);
        }

        // Combine the two stacks.
        List<ItemStack> all_stacks = new ArrayList<>();
        all_stacks.addAll(mod_stacks);
        all_stacks.addAll(custom_stacks);

        // Perform final sorting stuff.
        if (this.sort_criteria != SortCriteria.DEFAULT) this.sort_criteria.sort(all_stacks, player, this.sort_order);
        else if (this.sort_order == SortOrder.ASCENDING) Collections.reverse(all_stacks);

        // Return.
        return all_stacks;
    }

    /**
     * Add tab button.
     *
     * @author  Jade Godwin         <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    private void addTabButton(ScreenBuilder sb, int button_index, StatisticsTab tab) {
        // Add tab button.
        ButtonWidgetSlot tab_button = sb.addButton(button_index);
        tab_button.setTitle(tab.asString());
        if (selected_tab == tab)
            tab_button.setBackgroundType(ButtonWidgetSlot.BackgroundType.LEFT_TAB_SELECTED);
        else
            tab_button.setBackgroundType(ButtonWidgetSlot.BackgroundType.LEFT_TAB_UNSELECTED);
        tab_button.addOverlay(tab.getIcon());

        // Set up tab button listeners. All 3 buttons have the same function.
        SlotEventListener listener = (screen, slot) -> {
            this.selected_tab = tab; this.page = 1;
            while (!this.selected_tab.getAllowedSortCriteria().contains(this.sort_criteria)) { this.sort_criteria = sort_criteria.next(); }
            screen.replaceScreen(this);
        };
        tab_button.addLeftClickListener(listener);
        tab_button.addRightClickListener(listener);
        tab_button.addMiddleClickListener(listener);
    }

    /**
     * Add sort buttons.
     *
     * @author  Jade Godwin         <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    private void addSortButtons(ScreenBuilder sb, int button_index) {
        // Add criteria button.
        ButtonWidgetSlot criteria_button = sb.addButton(button_index);
        criteria_button.setTitle(this.sort_criteria.asString());
        criteria_button.setBackgroundType(ButtonWidgetSlot.BackgroundType.SMALL);
        criteria_button.addOverlay(BBSB.SORT_ICON);
        if (this.sort_criteria.getIcon() != null)
            criteria_button.addOverlay(this.sort_criteria.getIcon());

        // Set criteria button behavior. Middle click and left click have the same function.
        SlotEventListener left_click_criteria_behavior = (screen, slot) -> {
            do { this.sort_criteria = sort_criteria.next(); } while (!this.selected_tab.getAllowedSortCriteria().contains(this.sort_criteria));
            this.page = 1; screen.replaceScreen(this);
        };
        criteria_button.addLeftClickListener(left_click_criteria_behavior);
        criteria_button.addMiddleClickListener(left_click_criteria_behavior);
        criteria_button.addRightClickListener((screen, slot) -> {
            do { this.sort_criteria = sort_criteria.prev(); } while (!this.selected_tab.getAllowedSortCriteria().contains(this.sort_criteria));
            this.page = 1; screen.replaceScreen(this);
        });

        // Add order button.
        ButtonWidgetSlot order_button = sb.addButton(button_index + 1);
        order_button.setTitle(this.sort_order.asString());
        order_button.setBackgroundType(ButtonWidgetSlot.BackgroundType.SMALL);
        order_button.addOverlay(this.sort_order.getIcon());

        // Set order button behavior. Middle click and left click have the same function.
        SlotEventListener left_click_order_behavior = (screen, slot) -> {
            this.sort_order = sort_order.next();
            this.page = 1; screen.replaceScreen(this);
        };
        order_button.addLeftClickListener(left_click_order_behavior);
        order_button.addMiddleClickListener(left_click_order_behavior);
        order_button.addRightClickListener((screen, slot) -> {
            this.sort_order = sort_order.prev();
            this.page = 1; screen.replaceScreen(this);
        });
    }


    /**
     * Create the actual Screen instance.
     * Most of this code is lifted straight from v1 of the CreativeScreen in Blackblock Tools.
     *
     * @author  Jade Godwin         <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    @Override
    public ScreenBuilder getScreenBuilder() {
        ScreenBuilder sb = this.createBasicScreenBuilder("creative_input");
        sb.useFontTexture(new Identifier("blackblock", "gui/bb_stats"));
        sb.setCloneSlots(false);

        // Set display name.
        this.setDisplayName("Blackblock Statistics");

        // Add tab buttons.
        this.addTabButton(sb, 0, StatisticsTab.GENERAL);
        this.addTabButton(sb, 9, StatisticsTab.ITEMS);

        // Add sort buttons.
        this.addSortButtons(sb, 46);

        // Add items and return.
        switch (selected_tab) {
            case GENERAL -> this.addGeneralStatItems(sb);
            case ITEMS -> this.addItemItems(sb);
        }
        return sb;
    }



    /**
     * Add the items to the screen!
     * As in, the item items. The items with shit like broken, mined, used, dropped, etc.
     *
     * @author  Jade Godwin         <icanhasabanana@gmail.com>
     * @since    0.2.0
     */
    private void addItemItems(ScreenBuilder sb) {
        // Get selected items.
        List<Item> all_selected_items = this.getItems();

        // Get page slot info.
        int slots_per_page = 40;
        int item_count = all_selected_items.size();
        int start = (this.page - 1) * slots_per_page;
        int end = Math.min(start + slots_per_page, item_count);

        // Get subset of items.
        List<Item> items = all_selected_items.subList(start, end);

        // Fill the screen's slots.
        for (int i = 0; i < items.size(); i++) {
            // Create stack with statistics on it.
            ItemStack stack = new ItemStack(items.get(i));
            NbtList stats_lore = new NbtList();

            // Put the stat lore back into the item stack, then make item name yellow.
            NbtCompound display_nbt = new NbtCompound();
            display_nbt.put("Lore", stats_lore);
            stack.setSubNbt("display", display_nbt);
            stack.setCustomName(Text.translatable(stack.getTranslationKey()).setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withItalic(false)));

            // Create button stack.
            ButtonWidgetSlot button = sb.addButton(i + 1 + i / 8);
            button.setStack(stack);

            // Add on all the stats.
            button.setLore(Arrays.stream(new MutableText[]{
                Text.literal("- ").append(Text.literal("Times Mined: " + (items.get(i) instanceof BlockItem blockItem ? player.getStatHandler().getStat(Stats.MINED.getOrCreateStat(blockItem.getBlock())) : "0")).formatted(Formatting.WHITE)).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.YELLOW)),
                Text.literal("- ").append(Text.literal("Times Crafted: " + player.getStatHandler().getStat(Stats.CRAFTED.getOrCreateStat(items.get(i)))).formatted(Formatting.WHITE)).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.YELLOW)),
                Text.literal("- ").append(Text.literal("Times Used: " + player.getStatHandler().getStat(Stats.USED.getOrCreateStat(items.get(i)))).formatted(Formatting.WHITE)).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.YELLOW)),
                Text.literal("- ").append(Text.literal("Times Broken: " + player.getStatHandler().getStat(Stats.BROKEN.getOrCreateStat(items.get(i)))).formatted(Formatting.WHITE)).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.YELLOW)),
                Text.literal("- ").append(Text.literal("Picked Up: " + player.getStatHandler().getStat(Stats.PICKED_UP.getOrCreateStat(items.get(i)))).formatted(Formatting.WHITE)).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.YELLOW)),
                Text.literal("- ").append(Text.literal("Dropped: " + player.getStatHandler().getStat(Stats.DROPPED.getOrCreateStat(items.get(i)))).formatted(Formatting.WHITE)).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.YELLOW))
            }).toList());

            // Set up listener to send message in chat.
            SlotEventListener listener = (screen, slot) -> {
                // Send this item's stats in chat.
                player.sendMessage(Text.translatable(stack.getTranslationKey()).formatted(Formatting.YELLOW).append(Text.literal(" has the following statistics:").formatted(Formatting.WHITE)));
                if (stack.getItem() instanceof BlockItem blockItem) player.sendMessage(Text.literal("- ").formatted(Formatting.YELLOW).append(Text.literal("Times Mined: " + player.getStatHandler().getStat(Stats.MINED.getOrCreateStat(blockItem.getBlock()))).formatted(Formatting.WHITE)));
                else player.sendMessage(Text.literal("- ").formatted(Formatting.YELLOW).append(Text.literal("Times Mined: 0").formatted(Formatting.WHITE)));
                player.sendMessage(Text.literal("- ").formatted(Formatting.YELLOW).append(Text.literal("Times Crafted: " + player.getStatHandler().getStat(Stats.CRAFTED.getOrCreateStat(stack.getItem()))).formatted(Formatting.WHITE)));
                player.sendMessage(Text.literal("- ").formatted(Formatting.YELLOW).append(Text.literal("Times Used: " + player.getStatHandler().getStat(Stats.USED.getOrCreateStat(stack.getItem()))).formatted(Formatting.WHITE)));
                player.sendMessage(Text.literal("- ").formatted(Formatting.YELLOW).append(Text.literal("Times Broken: " + player.getStatHandler().getStat(Stats.BROKEN.getOrCreateStat(stack.getItem()))).formatted(Formatting.WHITE)));
                player.sendMessage(Text.literal("- ").formatted(Formatting.YELLOW).append(Text.literal("Picked Up: " + player.getStatHandler().getStat(Stats.PICKED_UP.getOrCreateStat(stack.getItem()))).formatted(Formatting.WHITE)));
                player.sendMessage(Text.literal("- ").formatted(Formatting.YELLOW).append(Text.literal("Dropped: " + player.getStatHandler().getStat(Stats.DROPPED.getOrCreateStat(stack.getItem()))).formatted(Formatting.WHITE)));
            };

            // Add listener to all 3 buttons.
            button.addLeftClickListener(listener);
            button.addMiddleClickListener(listener);
            button.addRightClickListener(listener);
        }

        // Add pagination!
        this.setUpPagination(sb, (int) Math.ceil(item_count / (double) slots_per_page));
    }

    private void addGeneralStatItems(ScreenBuilder sb) {
        // Get selected items.
        List<ItemStack> all_selected_items = this.getGeneralStats();

        // Get page slot info.
        int slots_per_page = 40;
        int item_count = all_selected_items.size();
        int start = (this.page - 1) * slots_per_page;
        int end = Math.min(start + slots_per_page, item_count);

        // Get subset of items.
        List<ItemStack> stacks = all_selected_items.subList(start, end);

        // Fill the screen's slots.
        for (int i = 0; i < stacks.size(); i++) {
            // Create stack with statistics on it.
            ItemStack stack = stacks.get(i);

            // Create button stack.
            ButtonWidgetSlot button = sb.addButton(i + 1 + i / 8);
            button.setStack(stack);

            // If stack has a custom_stat_owner attribute, add that to the lore.
            if (!stack.getNbt().getString("custom_stat_owner").isEmpty())
                button.setLore(Text.literal("ᴄᴜꜱᴛᴏᴍ ꜱᴛᴀᴛɪꜱᴛɪᴄ [" + stack.getNbt().getString("custom_stat_owner") + "]"));

            // Set up listener to send message in chat.
            SlotEventListener listener = (screen, slot) -> player.sendMessage(stack.getName());

            // Add listener to all 3 buttons.
            button.addLeftClickListener(listener);
            button.addMiddleClickListener(listener);
            button.addRightClickListener(listener);
        }

        // Add pagination!
        this.setUpPagination(sb, (int) Math.ceil(item_count / (double) slots_per_page));
    }

    /**
     * Register this screen (make all fonts)
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.2.0
     */
    public static ScreenBuilder registerScreen() {
        ScreenBuilder sb = new ScreenBuilder("creative_input");
        sb.useFontTexture(new Identifier("blackblock", "gui/bb_creative"));
        return sb;
    }

}
