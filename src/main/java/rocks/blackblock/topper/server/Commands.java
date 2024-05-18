package rocks.blackblock.topper.server;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import rocks.blackblock.core.commands.CommandCreator;
import rocks.blackblock.core.commands.CommandLeaf;
import rocks.blackblock.topper.creative.CreativeScreen;

public class Commands {

    private static final CommandLeaf BLACKBLOCK = CommandCreator.getPermissionRoot("blackblock", "blackblock.mod");

    public static void register() {
        addCreativeCommand();
    }

    /**
     * Add creative commands
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.1.0
     */
    private static void addCreativeCommand() {
        CommandLeaf creative_leaf = BLACKBLOCK.getChild("creative");
        creative_leaf.onExecute(context -> {

            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayer();

            if (player == null) { return 0; }

            player.openHandledScreen(new CreativeScreen(player));
            return 1;
        });
    }
}
