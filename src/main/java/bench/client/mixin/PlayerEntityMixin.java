package bench.client.mixin;

import bench.client.modules.AllModulesRegistry;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "getEntityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void onGetEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        double r = AllModulesRegistry.getCustomEntityReach();
        if (r > 0.0) cir.setReturnValue(r);
    }
}
