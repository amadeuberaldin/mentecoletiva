package br.com.amadeu.mentecoletiva;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class HiveMindMod implements ModInitializer {

    public static final int ROLE_DEFAULT = 0;
    public static final int ROLE_PRESSURE = 1;
    public static final int ROLE_BACKLINE = 2;
    public static final int ROLE_BREACHER = 3;

    private static final ThreadLocal<Boolean> HIVEMIND_PROPAGATING = ThreadLocal.withInitial(() -> false);

    public static boolean hivemind_isPropagating() {
        return HIVEMIND_PROPAGATING.get();
    }

    // Esta função define mobs que ficam sempre fora do hivemind.
    // Comunicação:
    // - usada por hivemind_canJoinForPlayer
    // - NÃO exclui skeleton, stray, bogged, creeper, witch e wither skeleton,
    //   pois esses agora dependem da progressão do player
    private static boolean hivemind_isAlwaysExcluded(LivingEntity entity) {
        return
                entity instanceof Spider
                || entity instanceof CaveSpider
                || entity instanceof EnderMan
                || entity instanceof Phantom
                || entity instanceof Silverfish
                || entity instanceof Endermite
                || entity instanceof Slime
                || entity instanceof MagmaCube
                || entity instanceof Guardian
                || entity instanceof ElderGuardian
                || entity instanceof Pillager
                || entity instanceof Vindicator
                || entity instanceof Evoker
                || entity instanceof Vex
                || entity instanceof Ravager
                || entity instanceof Blaze
                || entity instanceof Ghast
                || entity instanceof Hoglin
                || entity instanceof Zoglin
                || entity instanceof Piglin
                || entity instanceof PiglinBrute
                || entity instanceof ZombifiedPiglin
                || entity instanceof Shulker
                || entity instanceof Warden
                || entity instanceof WitherBoss
                || entity instanceof EnderDragon;
    }

    // Esta função verifica se o player está full diamante.
    // Comunicação:
    // - usada para liberar skeletons
    // - usada também na progressão de radius/maxJoin
    private static boolean hivemind_isFullDiamond(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(Items.DIAMOND_LEGGINGS)
                && player.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS);
    }

    // Esta função verifica se o player tem qualquer peça de netherite.
    // Comunicação:
    // - usada para liberar creepers
    // - usada também na progressão de radius/maxJoin
    private static boolean hivemind_hasAnyNetherite(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET)
                || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE)
                || player.getItemBySlot(EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS)
                || player.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS);
    }

    // Esta função verifica se o player está full netherite.
    // Comunicação:
    // - usada para liberar witch e wither skeleton
    private static boolean hivemind_isFullNetherite(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS)
                && player.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS);
    }

    // Estas funções contam as peças de armadura por material.
    // Comunicação:
    // - usadas na progressão dinâmica de radius e maxJoin
    private static int hivemind_countIronArmor(Player player) {
        int count = 0;

        if (player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)) count++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)) count++;
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(Items.IRON_LEGGINGS)) count++;
        if (player.getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS)) count++;

        return count;
    }

    private static int hivemind_countDiamondArmor(Player player) {
        int count = 0;

        if (player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET)) count++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE)) count++;
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(Items.DIAMOND_LEGGINGS)) count++;
        if (player.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS)) count++;

        return count;
    }

    private static int hivemind_countNetheriteArmor(Player player) {
        int count = 0;

        if (player.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET)) count++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE)) count++;
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS)) count++;
        if (player.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS)) count++;

        return count;
    }

    // Esta função define o radius dinâmico por progressão de armadura.
    // Regras:
    // - 0 a 1 peça de ferro -> 32
    // - 2 a 4 peças de ferro -> 48
    // - 1 a 3 peças de diamante -> 64
    // - full diamante OU qualquer peça de netherite até full netherite -> 128
    // Comunicação:
    // - usada por hivemind_callNearby
    private static double hivemind_getDynamicRadius(Player player) {
        int ironCount = hivemind_countIronArmor(player);
        int diamondCount = hivemind_countDiamondArmor(player);
        int netheriteCount = hivemind_countNetheriteArmor(player);

        if (diamondCount == 4 || netheriteCount > 0) {
            return 128.0;
        }

        if (diamondCount >= 1 && diamondCount <= 3) {
            return 64.0;
        }

        if (ironCount >= 2) {
            return 48.0;
        }

        return 16.0;
    }

    // Esta função define o maxJoin dinâmico por progressão de armadura.
    // Regras:
    // - 0 a 1 peça de ferro -> 10
    // - 2 a 4 peças de ferro -> 25
    // - 1 a 3 peças de diamante -> 40
    // - full diamante OU qualquer peça de netherite até full netherite -> 80
    // Comunicação:
    // - usada por hivemind_callNearby
    private static int hivemind_getDynamicMaxJoin(Player player) {
        int ironCount = hivemind_countIronArmor(player);
        int diamondCount = hivemind_countDiamondArmor(player);
        int netheriteCount = hivemind_countNetheriteArmor(player);

        if (diamondCount == 4 || netheriteCount > 0) {
            return 80;
        }

        if (diamondCount >= 1 && diamondCount <= 3) {
            return 40;
        }

        if (ironCount >= 2) {
            return 25;
        }

        return 10;
    }

    // Esta função define, por player, quais mobs podem entrar no hivemind.
    // Regras:
    // - base do mod: zumbis e variantes continuam
    // - full diamante: libera skeleton, stray e bogged
    // - qualquer peça de netherite: mantém skeletons e libera creeper
    // - full netherite: libera witch e wither skeleton
    // Comunicação:
    // - usada tanto no gatilho inicial quanto na seleção dos mobs próximos
    private static boolean hivemind_canJoinForPlayer(LivingEntity entity, Player player) {
        if (!(entity instanceof Enemy)) {
            return false;
        }

        if (hivemind_isAlwaysExcluded(entity)) {
            return false;
        }

        boolean fullDiamond = hivemind_isFullDiamond(player);
        boolean anyNetherite = hivemind_hasAnyNetherite(player);
        boolean fullNetherite = hivemind_isFullNetherite(player);

        if (entity instanceof Skeleton || entity instanceof Stray || entity instanceof Bogged || entity instanceof Spider) {
            return fullDiamond || anyNetherite;
        }

        if (entity instanceof Creeper) {
            return anyNetherite;
        }

        if (entity instanceof Witch || entity instanceof WitherSkeleton) {
            return fullNetherite;
        }

        return true;
    }

    @Override
    public void onInitialize() {
        // Este evento dispara o hivemind quando o player causa dano em um mob válido.
        // Comunicação:
        // - chama hivemind_canJoinForPlayer para validar o mob inicial
        // - chama hivemind_callNearby para propagar o ataque
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (LivingEntity entity, DamageSource source, float amount) -> {

                    Level w = entity.level();
                    if (!(w instanceof ServerLevel world))
                        return true;

                    Entity attackerEntity = source.getEntity();
                    if (!(attackerEntity instanceof Player player))
                        return true;

                    if (!hivemind_canJoinForPlayer(entity, player))
                        return true;

                    hivemind_callNearby(world, entity, player);

                    return true;
                });

        // Este evento recompensa mobs que morreram enquanto estavam ativos no hivemind.
        // Comunicação:
        // - depende de HiveMindFlag para verificar se o mob estava marcado
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
                        world.addFreshEntity(
                                new ItemEntity(
                                        world,
                                        entity.getX(),
                                        entity.getY(),
                                        entity.getZ(),
                                        new ItemStack(Items.EMERALD)
                                )
                        );
                    }
                });
    }

    // Esta função define o papel tático do mob dentro do hivemind.
    // Comunicação:
    // - usa SwarmRoleFlag para salvar o papel no mob
    // - esqueletos viram backline
    // - creepers viram breacher
    // - alguns outros mobs recebem pressure aleatoriamente
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

    // Esta função chama mobs próximos com radius e maxJoin dinâmicos por player.
    // Comunicação:
    // - usa hivemind_getDynamicRadius
    // - usa hivemind_getDynamicMaxJoin
    // - usa hivemind_canJoinForPlayer para filtrar por progressão
    // - usa hivemind_assignRole para distribuir papéis
    public static void hivemind_callNearby(ServerLevel world, LivingEntity center, Player player) {
        HIVEMIND_PROPAGATING.set(true);
        try {
            if (center instanceof HiveMindFlag flagCenter) {
                flagCenter.hivemind_setActiveTicks(200);
            }

            double radius = hivemind_getDynamicRadius(player);
            int maxJoin = hivemind_getDynamicMaxJoin(player);

            int joined = 0;

            for (Mob mob : world.getEntitiesOfClass(
                    Mob.class,
                    center.getBoundingBox().inflate(radius),
                    m -> m.isAlive()
                            && m != center
                            && hivemind_canJoinForPlayer(m, player))) {

                if (mob.isNoAi())
                    continue;

                mob.setTarget(player);

                if (mob instanceof HiveMindFlag flag) {
                    flag.hivemind_setActiveTicks(200);
                }

                hivemind_assignRole(mob);

                joined++;
                if (joined >= maxJoin)
                    break;
            }
        } finally {
            HIVEMIND_PROPAGATING.set(false);
        }
    }
}