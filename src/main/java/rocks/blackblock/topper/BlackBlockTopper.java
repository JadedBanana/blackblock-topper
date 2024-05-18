package rocks.blackblock.topper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rocks.blackblock.topper.compat.TopperEntrypoint;
import rocks.blackblock.topper.creative.CreativeScreen;
import rocks.blackblock.topper.creative.CreativeTab;
import rocks.blackblock.topper.server.Commands;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class BlackBlockTopper implements ModInitializer {

    public static final String MOD_ID = "blackblock";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);


    /**
     * Set the placement of an item on the creative screen
     *
     * @param   item        The item instance to register
     * @param   index       The index to put the item at
     * @param   tabs         The tab(s) to put the item on in the creative screen
     */
    public static void setCreativeScreenPlacement(Item item, int index, CreativeTab... tabs) {
        CreativeScreen.CREATIVE_ITEMS.put(index, item);
        Arrays.stream(tabs).toList().forEach(tab -> CreativeScreen.TAB_FILTERS.get(tab).add(item));
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
