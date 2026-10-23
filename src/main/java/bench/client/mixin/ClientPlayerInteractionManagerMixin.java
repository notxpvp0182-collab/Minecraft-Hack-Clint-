package bench.client.mixin;

import bench.client.modules.AllModulesRegistry;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Shadow private int blockBreakingCooldown;
    @Shadow private float currentBreakingProgress;

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        double mult = AllModulesRegistry.getFastBreakMultiplier();
        if (mult > 0.0) {
            this.blockBreakingCooldown = 0;
            if (mult > 1.0 && this.currentBreakingProgress > 0.0f) {
                this.currentBreakingProgress = Math.min(1.0f, this.currentBreakingProgress + (float) ((mult - 1.0) * 0.03));
            }
        }
    }
}
