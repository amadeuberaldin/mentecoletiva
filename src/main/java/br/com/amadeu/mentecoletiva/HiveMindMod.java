package br.com.amadeu.mentecoletiva;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.BoggedEntity;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.PiglinBruteEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.entity.mob.WitherSkeletonEntity;

public class HiveMindMod implements ModInitializer {

    private static final double RADIUS = 64.0;
    private static final int MAX_JOIN = 40;

    public static final int ROLE_DEFAULT = 0;
    public static final int ROLE_PRESSURE = 1;
    public static final int ROLE_BACKLINE = 2;
    public static final int ROLE_BREACHER = 3;

    private static final ThreadLocal<Boolean> HIVEMIND_PROPAGATING = ThreadLocal.withInitial(() -> false);

    public static boolean hivemind_isPropagating() {
        return HIVEMIND_PROPAGATING.get();
    }

    private static boolean hivemind_isExcluded(LivingEntity entity) {
        return

        // =========================
        // Overworld hostis clássicos
        // =========================
        entity instanceof SkeletonEntity
                || entity instanceof StrayEntity
                || entity instanceof BoggedEntity
                || entity instanceof CreeperEntity
                || entity instanceof SpiderEntity
                || entity instanceof CaveSpiderEntity
                || entity instanceof EndermanEntity
                || entity instanceof WitchEntity
                || entity instanceof PhantomEntity
                || entity instanceof SilverfishEntity
                || entity instanceof EndermiteEntity

                // =========================
                // Slimes
                // =========================
                || entity instanceof SlimeEntity
                || entity instanceof MagmaCubeEntity

                // =========================
                // Ocean mobs hostis
                // =========================
                || entity instanceof GuardianEntity
                || entity instanceof ElderGuardianEntity

                // =========================
                // Illagers / raid
                // =========================
                || entity instanceof PillagerEntity
                || entity instanceof VindicatorEntity
                || entity instanceof EvokerEntity
                || entity instanceof VexEntity
                || entity instanceof RavagerEntity

                // =========================
                // Nether hostis
                // =========================
                || entity instanceof BlazeEntity
                || entity instanceof GhastEntity
                || entity instanceof HoglinEntity
                || entity instanceof ZoglinEntity
                || entity instanceof PiglinEntity
                || entity instanceof PiglinBruteEntity
                || entity instanceof ZombifiedPiglinEntity
                || entity instanceof WitherSkeletonEntity

                // =========================
                // End / especiais / bosses
                // =========================
                || entity instanceof ShulkerEntity
                || entity instanceof WardenEntity
                || entity instanceof WitherEntity
                || entity instanceof EnderDragonEntity;
    }

    @Override
    public void onInitialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (LivingEntity entity, DamageSource source, float amount) -> {

                    World w = entity.getEntityWorld();
                    if (!(w instanceof ServerWorld world))
                        return true;

                    Entity attackerEntity = source.getAttacker();
                    if (!(attackerEntity instanceof PlayerEntity player))
                        return true;

                    if (!(entity instanceof HostileEntity))
                        return true;
                    if (hivemind_isExcluded(entity))
                        return true;

                    hivemind_callNearby(world, entity, player);

                    return true;
                });

        ServerLivingEntityEvents.AFTER_DEATH.register(
                (LivingEntity entity, DamageSource source) -> {

                    World w = entity.getEntityWorld();
                    if (!(w instanceof ServerWorld world))
                        return;

                    if (!(entity instanceof HiveMindFlag flag))
                        return;

                    if (flag.hivemind_getActiveTicks() <= 0)
                        return;

                    if (world.random.nextFloat() < 0.33f) {
                        entity.dropStack(world, new ItemStack(Items.EMERALD));
                    }
                });
    }

    public static void hivemind_assignRole(MobEntity mob) {
        if (!(mob instanceof SwarmRoleFlag roleFlag))
            return;

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
                            && m != center)) {
                if (mob.isAiDisabled())
                    continue;

                mob.setTarget(player);

                if (mob instanceof HiveMindFlag flag) {
                    flag.hivemind_setActiveTicks(200);
                }

                hivemind_assignRole(mob);

                joined++;
                if (joined >= MAX_JOIN)
                    break;
            }
        } finally {
            HIVEMIND_PROPAGATING.set(false);
        }
    }
}
