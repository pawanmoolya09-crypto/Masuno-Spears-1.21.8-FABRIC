package net.masuno.events;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.masuno.SpearBackport;
import net.masuno.network.SpearHitPayload;
import net.masuno.network.VelocityPayload;
import net.masuno.spear.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class ModEvents {
    public static Map<UUID, Integer> spearFrames = new HashMap<>();
    public static Map<UUID, Vec3d> playerVelocities = new HashMap<>();

    public static Vec3d getPlayerVelocity(ServerPlayerEntity player){
        Vec3d velocity = Vec3d.ZERO;
        if (playerVelocities.containsKey(player.getUuid())) velocity = playerVelocities.get(player.getUuid());

        return velocity;
    }

   public static void init() {
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> {
         for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (!player.isUsingItem() || !player.getMainHandStack().isIn(ModItems.SPEARS)) {
                    spearFrames.put(player.getUuid(), 0);
                }
                else if (spearFrames.containsKey(player.getUuid())) {
                    spearFrames.put(player.getUuid(), spearFrames.get(player.getUuid()) + 1);
                    float mult = SpearBackport.getMultiplier(player.getMainHandStack());
                    if (spearFrames.get(player.getUuid()) > SpearBackport.SpearDownedTime * mult)
                        if (!player.getItemCooldownManager().isCoolingDown(player.getMainHandStack())) player.getItemCooldownManager().set(player.getMainHandStack(),SpearBackport.getCooldown(player.getMainHandStack()));
                }
                else {
                    spearFrames.put(player.getUuid(), 0);
                }
            }
         }
      });

      ServerPlayNetworking.registerGlobalReceiver(VelocityPayload.ID, (payload, context) -> {
          playerVelocities.put(context.player().getUuid(),payload.velocity());
      });

      ServerPlayNetworking.registerGlobalReceiver(
         SpearHitPayload.ID,
         (payload, context) -> {
            if (context.player().getMainHandStack().isIn(ModItems.SPEARS)) {
               if (context.player().getWorld().getEntityById(payload.id()) instanceof LivingEntity livingEntity
                  && livingEntity.isInRange(context.player(), 4.0)
                  && livingEntity.hurtTime <= 0
                  && spearFrames.containsKey(context.player().getUuid())) {


                  float mult = SpearBackport.getMultiplier(context.player().getMainHandStack());
                  float power_mult = SpearBackport.getBasePower(context.player().getMainHandStack()) * 4.0F * (context.player().getWorld().getGameRules().getInt(SpearBackport.SPEAR_DAMAGE_PERCENTAGE) / 100f);
                  if ((float)spearFrames.get(context.player().getUuid()).intValue() >= (float)SpearBackport.maxSpearTime * mult) {
                     return;
                  }

                  DamageSource source = context.player().getWorld().getDamageSources().playerAttack(context.player());


                  float power = (float) getSpearPower(livingEntity, context.player());
                  boolean didDamage = livingEntity.damage(
                          context.player().getWorld(),
                          source,
                          (float) context.player().getAttributeValue(EntityAttributes.ATTACK_DAMAGE)  + power * power_mult);

                  if (!didDamage) return;

                  if (power > 1.4F) {
                     context.player()
                        .getWorld()
                        .playSound(null, context.player().getBlockPos(), SoundEvents.ITEM_MACE_SMASH_AIR, SoundCategory.PLAYERS, 0.2F, 1.8F);
                  }

                  if ((float) spearFrames.get(context.player().getUuid()) < (float)SpearBackport.maxSpearDismountTime * mult) {
                     livingEntity.dismountVehicle();
                  }

                  if ((float) spearFrames.get(context.player().getUuid()) < (float)SpearBackport.maxSpearKbTime * mult) {
                     Vec3d kb = context.player().getPos();
                     kb = kb.subtract(livingEntity.getPos());
                     livingEntity.takeKnockback(1.0, kb.normalize().x, kb.normalize().z);
                  }

                  context.player()
                     .getWorld()
                     .playSound(null, context.player().getBlockPos(), SoundEvents.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.0F, 0.48F);
               }
            }
         }
      );
   }

   public static double getSpearPower(LivingEntity victim,ServerPlayerEntity player) {
        Vec3d velocity = getPlayerVelocity(player);

        Vec3d victimVelocity = victim.getVelocity();
        if (victim instanceof ServerPlayerEntity victimPlayer){
            victimVelocity = getPlayerVelocity(victimPlayer);
        }

        Vec3d relativeVelocity = velocity.subtract(victimVelocity);
        double strength = relativeVelocity.length();
        double close_to_zero = Math.max(10 - strength, 0);
        strength += close_to_zero * 0.1;

        return strength;
   }
}
