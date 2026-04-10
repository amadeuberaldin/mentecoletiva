package br.com.amadeu.mentecoletiva;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;

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
        entity instanceof Skeleton
                || entity instanceof Stray
                || entity instanceof Bogged
                || entity instanceof Creeper
                || entity instanceof Spider
                || entity instanceof CaveSpider
                || entity instanceof EnderMan
                || entity instanceof Witch
                || entity instanceof Phantom
                || entity instanceof Silverfish
                || entity instanceof Endermite

                // =========================
                // Slimes
                // =========================
                || entity instanceof Slime
                || entity instanceof MagmaCube

                // =========================
                // Ocean mobs hostis
                // =========================
                || entity instanceof Guardian
                || entity instanceof ElderGuardian

                // =========================
                // Illagers / raid
                // =========================
                || entity instanceof Pillager
                || entity instanceof Vindicator
                || entity instanceof Evoker
                || entity instanceof Vex
                || entity instanceof Ravager

                // =========================
                // Nether hostis
                // =========================
                || entity instanceof Blaze
                || entity instanceof Ghast
                || entity instanceof Hoglin
                || entity instanceof Zoglin
                || entity instanceof Piglin
                || entity instanceof PiglinBrute
                || entity instanceof ZombifiedPiglin
                || entity instanceof WitherSkeleton

                // =========================
                // End / especiais / bosses
                // =========================
                || entity instanceof Shulker
                || entity instanceof Warden
                || entity instanceof WitherBoss
                || entity instanceof EnderDragon;
    }

    @Override
    public void onInitialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (LivingEntity entity, DamageSource source, float amount) -> {

                    Level w = entity.level();
                    if (!(w instanceof ServerLevel world))
                        return true;

                    Entity attackerEntity = source.getEntity();
                    if (!(attackerEntity instanceof Player player))
                        return true;

                    if (!(entity instanceof Enemy))
                        return true;
                    if (hivemind_isExcluded(entity))
                        return true;

                    hivemind_callNearby(world, entity, player);

                    return true;
                });

        ServerLivingEntityEvents.AFTER_DEATH.register(
                (LivingEntity entity, DamageSource source) -> {

                    Level w = entity.level();
                    if (!(w instanceof ServerLevel world))
                        return;

                    if (!(entity instanceof HiveMindFlag flag))
                        return;

                    if (flag.hivemind_getActiveTicks() <= 0)
                        return;

                    if (world.getRandom().nextFloat() < 0.33f) {
                        world.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(Items.EMERALD)));
                    }
                });
    }

    public static void hivemind_assignRole(Mob mob) {
        if (!(mob instanceof SwarmRoleFlag roleFlag))
            return;

        int role = ROLE_DEFAULT;

        if (mob instanceof AbstractSkeleton) {
            role = ROLE_BACKLINE;
        } else if (mob instanceof Creeper) {
            role = ROLE_BREACHER;
        } else {
            if (mob.getRandom().nextInt(4) == 0) {
                role = ROLE_PRESSURE;
            }
        }

        roleFlag.mentecoletiva_setRole(role);
    }

    public static void hivemind_callNearby(ServerLevel world, LivingEntity center, Player player) {
        HIVEMIND_PROPAGATING.set(true);
        try {
            if (center instanceof HiveMindFlag flagCenter) {
                flagCenter.hivemind_setActiveTicks(200);
            }

            int joined = 0;

            for (Mob mob : world.getEntitiesOfClass(
                    Mob.class,
                    center.getBoundingBox().inflate(RADIUS),
                    m -> (m instanceof Enemy)
                            && !hivemind_isExcluded(m)
                            && m.isAlive()
                            && m != center)) {
                if (mob.isNoAi())
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