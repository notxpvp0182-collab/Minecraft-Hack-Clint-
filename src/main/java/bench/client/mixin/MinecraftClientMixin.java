package bench.client.mixin;

import bench.client.core.ClientDataManager;
import bench.client.modules.AllModulesRegistry;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    private int itemUseCooldown;

    @Shadow
    public abstract void tick();

    @Unique
    private double benchTimerAccumulator = 0.0;

    @Unique
    private boolean benchInExtraTick = false;

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        ClientDataManager.recordLeftClick();
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUse(CallbackInfo ci) {
        ClientDataManager.recordRightClick();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        int maxPlaceCd = AllModulesRegistry.getFastPlaceMaxCooldown();
        if (maxPlaceCd >= 0 && this.itemUseCooldown > maxPlaceCd) {
            this.itemUseCooldown = maxPlaceCd;
        }

        if (!benchInExtraTick) {
            double timerMult = AllModulesRegistry.getTimerSpeedMultiplier();
            if (timerMult > 1.0) {
                benchTimerAccumulator += (timerMult - 1.0);
                if (benchTimerAccumulator >= 1.0) {
                    benchTimerAccumulator -= 1.0;
                    benchInExtraTick = true;
                    try {
                        this.tick();
                    } finally {
                        benchInExtraTick = false;
                    }
                }
            }
        }
    }
}
