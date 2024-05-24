package rocks.blackblock.topper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rocks.blackblock.core.BlackBlockCore;
import rocks.blackblock.core.component.Component;
import rocks.blackblock.core.component.ComponentKey;
import rocks.blackblock.topper.compat.TopperEntrypoint;
import rocks.blackblock.topper.creative.CreativeScreen;
import rocks.blackblock.topper.creative.CreativeTab;
import rocks.blackblock.topper.server.Commands;
import rocks.blackblock.topper.statistics.CustomStatisticsComponent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

@SuppressWarnings("unused")
public class BlackBlockTopper implements ModInitializer {

    public static final String MOD_ID = "blackblock";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    // Creative items.
    public static final TreeMap<Integer, Item> CREATIVE_ITEMS = new TreeMap<>();
    public static List<Item> CREATIVE_ITEMS_FLATTENED = null;
    public static final HashMap<Identifier, Item> STAT_ITEMS = new HashMap<>();

    // Custom statistics component
    public static ComponentKey.Global<CustomStatisticsComponent> CUSTOM_STATS = Component.Global.register(BlackBlockCore.id("custom_stats"), CustomStatisticsComponent.class, CustomStatisticsComponent::new);


    /**
     * Set the placement of an item on the creative screen
     *
     * @param   item        The item instance to register
     * @param   index       The index to put the item at
     * @param   tabs         The tab(s) to put the item on in the creative screen
     */
    public static void setCreativeScreenPlacement(Item item, int index, CreativeTab... tabs) {
        CREATIVE_ITEMS.put(index, item);
        Arrays.stream(tabs).toList().forEach(tab -> CreativeScreen.TAB_FILTERS.get(tab).add(item));
    }

    /**
     * Add a statistic to the Statistics screen
     *
     * @param   statistic_id    The statistic identifier
     * @param   icon            The item to use as an icon for this stat
     */
    public static void setStatisticsScreenPlacement(Identifier statistic_id, Item icon) {
        STAT_ITEMS.put(statistic_id, icon);
    }

    @Override
    public void onInitialize() {
        // Initialize the maps that need data filled out
        CreativeScreen.initialize();

        // Get each topper entrypoint and have them register their info
        List<TopperEntrypoint> entrypoints = FabricLoader.getInstance().getEntrypoints("blackblock-topper", TopperEntrypoint.class);
        for (TopperEntrypoint entrypointEntry : entrypoints) {
            entrypointEntry.registerTopperInfo(this);
        }

        // Register screens
        if (FabricLoader.getInstance().isModLoaded("polymc")) {
            CreativeScreen.registerScreen();
        }

        // Register commands
        Commands.register();
    }
}
