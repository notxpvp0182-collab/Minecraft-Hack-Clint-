package bench.client.mixin;

import bench.client.modules.AllModulesRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityStepMixin {
    @Inject(method = "getStepHeight", at = @At("RETURN"), cancellable = true)
    private void onGetStepHeight(CallbackInfoReturnable<Float> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && (Object) this == client.player) {
            float customStep = AllModulesRegistry.getCustomStepHeight();
            if (customStep > 0.0f) {
                cir.setReturnValue(customStep);
            }
        }
    }
}
