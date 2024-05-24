package rocks.blackblock.topper.creative;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
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
 * The v2 of Blackblock's creative screen.
 * Dynamically displays items in multiple tabs with sorting options.
 *
 * @author  Jade Godwin         <icanhasabanana@gmail.com>
 * @since    0.1.0
 */
public class CreativeScreen extends ItemBrowsingScreen {

    private static boolean INITIALIZED = false;

    public static final HashMap<CreativeTab, ArrayList<Item>> TAB_FILTERS = new HashMap<>();

    private CreativeTab selected_tab = CreativeTab.ALL;
    private SortCriteria sort_criteria = SortCriteria.DEFAULT;
    private SortOrder sort_order = SortOrder.DESCENDING;

    public CreativeScreen(ServerPlayerEntity player) {
        super();
        this.player = player;
    }

    /**
     * Create filter lists.
     *
     * @author  Jade Godwin         <icanhasabanana@gmail.com>
     * @since    0.1.0
     */
    public static void initialize() {
        // Make sure we don't run more than once!
        if (INITIALIZED) return;
        INITIALIZED = true;

        // Initialize the tab filters.
        TAB_FILTERS.put(CreativeTab.BUILDING_BLOCKS, new ArrayList<>());
        TAB_FILTERS.put(CreativeTab.FUNCTIONAL_BLOCKS, new ArrayList<>());
        TAB_FILTERS.put(CreativeTab.COSMETICS, new ArrayList<>());
        TAB_FILTERS.put(CreativeTab.MOBHEADS, new ArrayList<>());
        TAB_FILTERS.put(CreativeTab.FOOD, new ArrayList<>());
        TAB_FILTERS.put(CreativeTab.MINIGAME_ITEMS, new ArrayList<>());
        TAB_FILTERS.put(CreativeTab.OTHER_ITEMS, new ArrayList<>());
    }

    /**
     * Get all the custom items & blocks
     * Ordered by the current sort order and filtered by the current tab.
     *
     * @author  Jade Godwin         <icanhasabanana@gmail.com>
     * @since    0.1.0
     */
    private List<Item> getItems() {
        // If all items does not yet exist, make it.
        if (BlackBlockTopper.CREATIVE_ITEMS_FLATTENED == null)
            BlackBlockTopper.CREATIVE_ITEMS_FLATTENED = BlackBlockTopper.CREATIVE_ITEMS.values().stream().toList();

        // If we're on the all tab, just return all items.
        ArrayList<Item> returned_items = new ArrayList<>();
        if (selected_tab == CreativeTab.ALL) {
            returned_items.addAll(BlackBlockTopper.CREATIVE_ITEMS_FLATTENED);

        // Otherwise, filter based on the tab.
        } else {
            ArrayList<Item> filter = TAB_FILTERS.get(selected_tab);
            BlackBlockTopper.CREATIVE_ITEMS_FLATTENED.forEach(item -> { if (filter.contains(item)) { returned_items.add(item); } });
        }

        // Implement sort criteria & return.
        this.sort_criteria.sort(returned_items, this.sort_order, this.player);
        return returned_items;
    }

    /**
     * Add tab button.
     *
     * @author  Jade Godwin         <icanhasabanana@gmail.com>
     * @since    0.1.0
     */
    private void addTabButton(ScreenBuilder sb, int button_index, CreativeTab tab) {
        // Add tab button.
        ButtonWidgetSlot tab_button = sb.addButton(button_index);
        tab_button.setTitle(tab.asString());
        if (selected_tab == tab)
            tab_button.setBackgroundType(ButtonWidgetSlot.BackgroundType.TOP_TAB_SELECTED);
        else
            tab_button.setBackgroundType(ButtonWidgetSlot.BackgroundType.TOP_TAB_UNSELECTED);
        tab_button.addOverlay(tab.getIcon());

        // Set up tab button listeners. All 3 buttons have the same function.
        SlotEventListener listener = (screen, slot) -> {
            this.selected_tab = tab; this.page = 1;
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
     * @since    0.1.0
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
     * @since    0.1.0
     */
    @Override
    public ScreenBuilder getScreenBuilder() {
        // Initialize screen.
        ScreenBuilder sb = this.createBasicScreenBuilder("creative_input");
        sb.useFontTexture(new Identifier("blackblock", "gui/bb_creative"));
        sb.setCloneSlots(false);
        this.setDisplayName("Blackblock Creative");

        // Add tab buttons.
        this.addTabButton(sb, 0, CreativeTab.BUILDING_BLOCKS);
        this.addTabButton(sb, 1, CreativeTab.FUNCTIONAL_BLOCKS);
        this.addTabButton(sb, 2, CreativeTab.COSMETICS);
        this.addTabButton(sb, 3, CreativeTab.MOBHEADS);
        this.addTabButton(sb, 4, CreativeTab.FOOD);
        this.addTabButton(sb, 5, CreativeTab.MINIGAME_ITEMS);
        this.addTabButton(sb, 6, CreativeTab.OTHER_ITEMS);
        this.addTabButton(sb, 8, CreativeTab.ALL);

        // Add sort buttons.
        this.addSortButtons(sb, 45);

        // Add items.
        this.addItems(sb);
        return sb;
    }

    /**
     * Add the items to the screen!
     *
     * @author  Jade Godwin         <icanhasabanana@gmail.com>
     * @since    0.1.1
     */
    private void addItems(ScreenBuilder sb) {
        // Get selected items.
        List<Item> all_selected_items = this.getItems();

        // Get page slot info.
        int slots_per_page = 36;
        int item_count = all_selected_items.size();
        int start = (this.page - 1) * slots_per_page;
        int end = Math.min(start + slots_per_page, item_count);

        // Get subset of items.
        List<Item> items = all_selected_items.subList(start, end);

        // Fill the screen's slots.
        for (int i = 0; i < items.size(); i++) {
            // Create button stack.
            ItemStack stack = new ItemStack(items.get(i));
            ButtonWidgetSlot button = sb.addButton(i + 9);
            button.setStack(stack);

            // Add listener to left click.
            button.addLeftClickListener((screen, slot) -> {
                // If player clicks on tile with an item in hand already, delete it.
                ItemStack old_stack = screen.getCursorStack();
                if (old_stack != null && !old_stack.isEmpty()) {
                    old_stack.setCount(0);
                    return;
                }

                // Give them the new stack.
                ItemStack new_stack = stack.copy();
                if (screen.isPressingShift())
                    new_stack.setCount(new_stack.getMaxCount());
                screen.setCursorStack(new_stack);
            });

            // Add listener to right click.
            button.addRightClickListener((screen, slot) -> {
                // If player clicks on tile with an item in hand already, decrease its count by 1.
                ItemStack old_stack = screen.getCursorStack();
                if (old_stack != null && !old_stack.isEmpty()) {
                    old_stack.decrement(1);
                    return;
                }

                // Give them the new stack.
                ItemStack new_stack = stack.copy();
                if (screen.isPressingShift())
                    new_stack.setCount(new_stack.getMaxCount());
                screen.setCursorStack(new_stack);
            });

            // Add listener to middle click.
            button.addMiddleClickListener((screen, slot) -> {
                // If player clicks on tile with an item in hand already, return.
                ItemStack old_stack = screen.getCursorStack();
                if (old_stack != null && !old_stack.isEmpty()) return;

                // Give them the new stack with the max count already there.
                ItemStack new_stack = stack.copy();
                new_stack.setCount(new_stack.getMaxCount());
                screen.setCursorStack(new_stack);
            });
        }

        // Add pagination!
        this.setUpPagination(sb, (int) Math.ceil(item_count / (double) slots_per_page));
    }

    /**
     * Register this screen (make all fonts)
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.1.0
     */
    public static ScreenBuilder registerScreen() {
        ScreenBuilder sb = new ScreenBuilder("creative_input");
        sb.useFontTexture(new Identifier("blackblock", "gui/bb_creative"));
        return sb;
    }

}
