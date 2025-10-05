package net.masuno.events;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.masuno.network.SpearHitPayload;
import net.masuno.network.VelocityPayload;
import net.masuno.spear.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

@Environment(EnvType.CLIENT)
public class ModEventsClient {
   private static boolean wasPressed = false;
   public static boolean isHoldingSpear = false;
   public static int ticksHoldingSpear = 0;

   public static void init() {
      ClientTickEvents.START_CLIENT_TICK
         .register(
            (StartTick)client -> {
               if (client.player != null) {
                   SyncVelocity(client);

                   if (client.player.getAttackCooldownProgress(1.25F) < 1.0F) {
                       return;
                   }

                   if (client.player.getItemCooldownManager().isCoolingDown(client.player.getMainHandStack())) {
                       if (isHoldingSpear) {
                           unHoldSpear(client);
                           isHoldingSpear = false;
                       }

                       return;
                   }
                   if (client.player.isBlocking()) return;
                   if (client.player.getMainHandStack().isIn(ModItems.SPEARS) && client.options.useKey.isPressed()) {
                       if (!wasPressed) {
                           isHoldingSpear = true;
                       }

                       wasPressed = true;
                   } else {
                       unHoldSpear(client);
                       isHoldingSpear = false;
                       wasPressed = false;
                   }

                   if (isHoldingSpear) {
                       ticksHoldingSpear++;
                   }
               }
            }
         );
   }

   public static void unHoldSpear(MinecraftClient client) {
      ticksHoldingSpear = 0;
      if (isHoldingSpear) {
          assert client.player != null;
          client.player.attack(client.player);
      }
   }
    public static void SyncVelocity(MinecraftClient client){
        assert client.player != null;
        if (client.player.getControllingVehicle() != null){
            ClientPlayNetworking.send(new VelocityPayload(client.player.getControllingVehicle().getVelocity()));
        }else {
            ClientPlayNetworking.send(new VelocityPayload(client.player.getVelocity()));
        }
    }
   public static void SpearAttack(MinecraftClient client, LivingEntity entity) {
      assert client.player != null;
      if (client.player.getControllingVehicle() == entity) return;
      client.player.swingHand(Hand.MAIN_HAND);
      ClientPlayNetworking.send(new SpearHitPayload(entity.getId()));
   }

    public static void attack(HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHitResult
                && ticksHoldingSpear >= 3
                && entityHitResult.getEntity() instanceof LivingEntity le
                && le.hurtTime <= 0) {
            SpearAttack(MinecraftClient.getInstance(), le);
        }
    }
}
