package net.masuno.mixin;

import net.masuno.spear.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerEntity.class})
public abstract class ClientPlayerMixin {

    @Shadow @Final protected MinecraftClient client;

    @Shadow protected abstract boolean isBlind();

    @Shadow protected abstract boolean canSprint();

    @Shadow protected abstract boolean canVehicleSprint(Entity vehicle);

    @Shadow private boolean falling;

    @Inject(
      method = {"applyMovementSpeedFactors"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void InjectMovementSpear(Vec2f input, CallbackInfoReturnable<Vec2f> cir) {
       if (input.lengthSquared() == 0.0F) {
           cir.setReturnValue(input);
       } else {
           Vec2f vec2f = input.multiply(0.98F);
           if (((ClientPlayerEntity)(Object)this).isUsingItem() && !((ClientPlayerEntity)(Object)this).hasVehicle()) {
               if (!((ClientPlayerEntity)(Object)this).getActiveItem().isIn(ModItems.SPEARS)) vec2f = vec2f.multiply(0.2F);
           }

           if (((ClientPlayerEntity)(Object)this).shouldSlowDown()) {
               float f = (float)((ClientPlayerEntity)(Object)this).getAttributeValue(EntityAttributes.SNEAKING_SPEED);
               vec2f = vec2f.multiply(f);
           }

           cir.setReturnValue(applyDirectionalMovementSpeedFactors(vec2f));
       }
   }

   @Inject(method = "canStartSprinting", at = @At("HEAD"),cancellable = true)
   private void InjectCanSprint(CallbackInfoReturnable<Boolean> cir){
        boolean result;
        if (client.player == null) return;

        if (!client.player.getMainHandStack().isIn(ModItems.SPEARS)){
            result = !((ClientPlayerEntity)(Object)this).isSprinting()
                    && ((ClientPlayerEntity)(Object)this).input.hasForwardMovement()
                    && canSprint()
                    && !((ClientPlayerEntity)(Object)this).isUsingItem()
                    && !isBlind()
                    && (!((ClientPlayerEntity)(Object)this).hasVehicle() || canVehicleSprint(((ClientPlayerEntity)(Object)this).getVehicle()))
                    && (!((ClientPlayerEntity)(Object)this).isGliding() || ((ClientPlayerEntity)(Object)this).isSubmergedInWater())
                    && (!((ClientPlayerEntity)(Object)this).shouldSlowDown() || ((ClientPlayerEntity)(Object)this).isSubmergedInWater())
                    && (!((ClientPlayerEntity)(Object)this).isTouchingWater() || ((ClientPlayerEntity)(Object)this).isSubmergedInWater());
        }else {
            result = !((ClientPlayerEntity)(Object)this).isSprinting()
                    && ((ClientPlayerEntity)(Object)this).input.hasForwardMovement()
                    && canSprint()
                    && !isBlind()
                    && (!((ClientPlayerEntity)(Object)this).hasVehicle() || canVehicleSprint(((ClientPlayerEntity)(Object)this).getVehicle()))
                    && (!((ClientPlayerEntity)(Object)this).isGliding() || ((ClientPlayerEntity)(Object)this).isSubmergedInWater())
                    && (!((ClientPlayerEntity)(Object)this).shouldSlowDown() || ((ClientPlayerEntity)(Object)this).isSubmergedInWater())
                    && (!((ClientPlayerEntity)(Object)this).isTouchingWater() || ((ClientPlayerEntity)(Object)this).isSubmergedInWater());
        }
        cir.setReturnValue(result);
   }

    @Unique
    private static Vec2f applyDirectionalMovementSpeedFactors(Vec2f vec) {
        float f = vec.length();
        if (f <= 0.0F) {
            return vec;
        } else {
            Vec2f vec2f = vec.multiply(1.0F / f);
            float g = getDirectionalMovementSpeedMultiplier(vec2f);
            float h = Math.min(f * g, 1.0F);
            return vec2f.multiply(h);
        }
    }

    @Unique
    private static float getDirectionalMovementSpeedMultiplier(Vec2f vec) {
        float f = Math.abs(vec.x);
        float g = Math.abs(vec.y);
        float h = g > f ? f / g : g / f;
        return MathHelper.sqrt(1.0F + MathHelper.square(h));
    }
}
