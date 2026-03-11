package br.com.amadeu.mentecoletiva;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class HiveMindMod implements ModInitializer {

    private static final double RADIUS = 64.0;
    private static final int MAX_JOIN = 40;

    public static final int ROLE_DEFAULT = 0;
    public static final int ROLE_PRESSURE = 1;
    public static final int ROLE_BACKLINE = 2;
    public static final int ROLE_BREACHER = 3;

    private static final ThreadLocal<Boolean> HIVEMIND_PROPAGATING =
            ThreadLocal.withInitial(() -> false);

    public static boolean hivemind_isPropagating() {
        return HIVEMIND_PROPAGATING.get();
    }

    private static boolean hivemind_isExcluded(LivingEntity entity) {
        return entity instanceof EndermanEntity
                || entity instanceof ZombifiedPiglinEntity
                || entity instanceof WardenEntity
                || entity instanceof WitherEntity;
    }

    @Override
    public void onInitialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
            (LivingEntity entity, DamageSource source, float amount) -> {

                World w = entity.getEntityWorld();
                if (!(w instanceof ServerWorld world)) return true;

                Entity attackerEntity = source.getAttacker();
                if (!(attackerEntity instanceof PlayerEntity player)) return true;

                if (!(entity instanceof HostileEntity)) return true;
                if (hivemind_isExcluded(entity)) return true;

                hivemind_callNearby(world, entity, player);

                return true;
            }
        );
    }

    public static void hivemind_assignRole(MobEntity mob) {
        if (!(mob instanceof SwarmRoleFlag roleFlag)) return;

        int role = ROLE_DEFAULT;

        if (mob instanceof AbstractSkeletonEntity) {
            role = ROLE_BACKLINE;
        } else if (mob instanceof CreeperEntity) {
            role = ROLE_BREACHER;
        } else {
            if (mob.getRandom().nextInt(4) == 0) {
                role = ROLE_PRESSURE;
            }
        }

        roleFlag.mentecoletiva_setRole(role);
    }

    public static void hivemind_callNearby(ServerWorld world, LivingEntity center, PlayerEntity player) {
        HIVEMIND_PROPAGATING.set(true);
        try {
            if (center instanceof HiveMindFlag flagCenter) {
                flagCenter.hivemind_setActiveTicks(200);
            }

            int joined = 0;

            for (MobEntity mob : world.getEntitiesByClass(
                    MobEntity.class,
                    center.getBoundingBox().expand(RADIUS),
                    m -> (m instanceof HostileEntity)
                            && !hivemind_isExcluded(m)
                            && m.isAlive()
                            && m != center
            )) {
                if (mob.isAiDisabled()) continue;

                mob.setTarget(player);

                if (mob instanceof HiveMindFlag flag) {
                    flag.hivemind_setActiveTicks(200);
                }

                hivemind_assignRole(mob);

                joined++;
                if (joined >= MAX_JOIN) break;
            }
        } finally {
            HIVEMIND_PROPAGATING.set(false);
        }
    }
}
