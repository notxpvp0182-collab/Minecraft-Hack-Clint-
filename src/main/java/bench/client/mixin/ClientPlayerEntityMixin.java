package bench.client.mixin;

import bench.client.modules.AllModulesRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class ClientPlayerEntityMixin {
    @Shadow public abstract void setVelocity(double x, double y, double z);
    @Shadow public abstract Vec3d getVelocity();

    @Inject(method = "setVelocityClient", at = @At("HEAD"), cancellable = true)
    private void onSetVelocityClient(Vec3d clientVelocity, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && (Object) this == client.player && AllModulesRegistry.isVelocityEnabled()) {
            double h = AllModulesRegistry.getVelocityHorizontal();
            double v = AllModulesRegistry.getVelocityVertical();
            Vec3d cur = this.getVelocity();
            this.setVelocity(cur.x + (clientVelocity.x - cur.x) * h, cur.y + (clientVelocity.y - cur.y) * v, cur.z + (clientVelocity.z - cur.z) * h);
            ci.cancel();
        }
    }
}
