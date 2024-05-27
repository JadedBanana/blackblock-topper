package rocks.blackblock.topper.mixin;

import com.google.common.collect.Sets;
import net.minecraft.entity.LivingEntity;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.Stat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.topper.BlackBlockTopper;

import java.util.Set;

@Mixin(ServerStatHandler.class)
public class ServerStatHandlerMixin {

    @Shadow @Final private Set<Stat<?>> pendingStats;

    @Inject(method="takePendingStats", at = @At("HEAD"))
    private void takePendingStatsMixin(CallbackInfoReturnable<Set<Stat<?>>> cir) {
        // Create a copy of the pendingStats list.
        Set<Stat<?>> server_stats = Sets.newHashSet();
        server_stats.addAll(this.pendingStats);

        // Iterate through and, for any stat that doesn't have ":minecraft." in it, remove that stat from the pending stats list.
        server_stats.forEach(stat -> {
            if (!stat.getName().contains(":minecraft."))
                this.pendingStats.remove(stat);
        });
    }
}
