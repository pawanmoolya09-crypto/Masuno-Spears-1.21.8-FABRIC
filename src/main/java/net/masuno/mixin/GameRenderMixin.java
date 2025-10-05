package net.masuno.mixin;

import net.masuno.events.ModEventsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public abstract class GameRenderMixin {
    @Shadow protected abstract HitResult findCrosshairTarget(Entity camera, double blockInteractionRange, double entityInteractionRange, float tickProgress);

    @Shadow @Final private MinecraftClient client;

    @Redirect(method = "updateCrosshairTarget",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;findCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;"))
    private HitResult RedirectCrosshairCheck(GameRenderer instance, Entity camera, double blockInteractionRange, double entityInteractionRange, float tickProgress){
        HitResult hit = findCrosshairTarget(camera, blockInteractionRange, entityInteractionRange, tickProgress);
        if (client.crosshairTarget != hit){
            ModEventsClient.attack(hit);
        }
        return hit;
    }
}
