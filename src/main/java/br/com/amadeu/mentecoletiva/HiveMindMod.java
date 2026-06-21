package br.com.amadeu.mentecoletiva;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
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
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.cubemob.Slime;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.EntitySpawnReason;

public class HiveMindMod implements ModInitializer {

    public static final int ROLE_DEFAULT = 0;
    public static final int ROLE_PRESSURE = 1;
    public static final int ROLE_BACKLINE = 2;
    public static final int ROLE_BREACHER = 3;

    private static final ThreadLocal<Boolean> HIVEMIND_PROPAGATING = ThreadLocal.withInitial(() -> false);

    /*
     * Guarda o tick em que a janela ativa começou para cada jogador.
     *
     * A partir desse tick calculamos:
     * - os 2 minutos de atividade
     * - os 40 minutos de cooldown
     *
     * Comunicação:
     * - usado por hivemind_isPlayerActive
     * - usado por hivemind_isPlayerOnCooldown
     * - usado por hivemind_tryActivateForPlayer
     */
    private static final Map<UUID, Long> ACTIVE_START_TICK = new ConcurrentHashMap<>();

    /*
     * 2 minutos de janela ativa.
     */
    private static final long ACTIVE_DURATION_TICKS = 5L * 60L * 20L;

    /*
     * 40 minutos de cooldown.
     */
    private static final long COOLDOWN_DURATION_TICKS = 30L * 60L * 20L;

    /*
     * Duração total do ciclo:
     * - 2 min ativos
     * - 40 min de cooldown
     */
    private static final long TOTAL_CYCLE_TICKS = ACTIVE_DURATION_TICKS + COOLDOWN_DURATION_TICKS;

    public static boolean hivemind_isPropagating() {
        return HIVEMIND_PROPAGATING.get();
    }

    /*
     * Retorna o tick atual do mundo.
     *
     * Comunicação:
     * - usado pelas rotinas de tempo do player
     */
    private static long hivemind_now(ServerLevel world) {
        return world.getGameTime();
    }

    private static void hivemind_registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("hivemind")
                            .executes(context -> {
                                context.getSource().sendSuccess(
                                        () -> Component.literal("§5[HiveMind] §7Evento está: "
                                                + (HIVEMIND_EVENT_ENABLED ? "§aLIGADO" : "§cDESLIGADO")),
                                        false);
                                return 1;
                            })
                            .then(Commands.literal("on")
                                    .requires(HiveMindMod::hivemind_isOp)
                                    .executes(context -> {
                                        HIVEMIND_EVENT_ENABLED = true;
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("§5[HiveMind] §aEvento ligado."),
                                                true);
                                        return 1;
                                    }))
                            .then(Commands.literal("off")
                                    .requires(HiveMindMod::hivemind_isOp)
                                    .executes(context -> {
                                        HIVEMIND_EVENT_ENABLED = false;
                                        ACTIVE_START_TICK.clear();
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("§5[HiveMind] §cEvento desligado."),
                                                true);
                                        return 1;
                                    })));
        });
    }

    private static boolean hivemind_isOp(CommandSourceStack source) {

        if (source.getPlayer() instanceof ServerPlayer player) {

            return source.getServer()
                    .getPlayerList()
                    .isOp(new NameAndId(player.getGameProfile()));
        }

        return true;
    }

    /*
     * Retorna quantos ticks ainda restam na janela ativa do Hivemind.
     *
     * Comunicação:
     * - usado pelo contador do action bar
     */
    private static long hivemind_getRemainingActiveTicks(ServerLevel world, Player player) {
        Long startTick = ACTIVE_START_TICK.get(player.getUUID());
        if (startTick == null) {
            return 0L;
        }

        long elapsed = hivemind_now(world) - startTick;
        long remaining = ACTIVE_DURATION_TICKS - elapsed;
        return Math.max(0L, remaining);
    }

    private static final String WORLD_Z_ID = "mundoz:world_z";
    private static boolean HIVEMIND_EVENT_ENABLED = false;

    private static boolean hivemind_isWorldZ(Level level) {
        return level.dimension().identifier().toString().equals(WORLD_Z_ID);
    }

    /*
     * Formata ticks em mm:ss.
     *
     * Comunicação:
     * - usado para o texto do action bar
     */
    private static String hivemind_formatTicksAsMinutesSeconds(long ticks) {
        long totalSeconds = ticks / 20L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%d:%02d", minutes, seconds);
    }

    /*
     * Envia o contador do Hivemind no action bar.
     *
     * Comunicação:
     * - chamado no tick do servidor enquanto a janela ativa existir
     */
    private static void hivemind_sendActionBar(ServerLevel world, ServerPlayer player) {
        long remainingTicks = hivemind_getRemainingActiveTicks(world, player);
        if (remainingTicks <= 0L) {
            return;
        }

        String timeText = hivemind_formatTicksAsMinutesSeconds(remainingTicks);

        player.sendSystemMessage(
                Component.literal("Sobreviva: " + timeText).withStyle(ChatFormatting.DARK_RED),
                true);
    }

    /*
     * Verifica se o jogador está dentro da janela ativa de 2 minutos.
     *
     * Comunicação:
     * - usado por hivemind_tryActivateForPlayer
     */
    public static boolean hivemind_isPlayerActive(ServerLevel world, Player player) {
        Long startTick = ACTIVE_START_TICK.get(player.getUUID());
        if (startTick == null) {
            return false;
        }

        long elapsed = hivemind_now(world) - startTick;
        return elapsed >= 0 && elapsed < ACTIVE_DURATION_TICKS;
    }

    /*
     * Verifica se o jogador está no cooldown.
     *
     * Regras:
     * - o cooldown começa imediatamente quando os 2 min acabam
     * - e dura até completar 42 min desde o início da ativação
     *
     * Comunicação:
     * - usado por hivemind_tryActivateForPlayer
     */
    public static boolean hivemind_isPlayerOnCooldown(ServerLevel world, Player player) {
        Long startTick = ACTIVE_START_TICK.get(player.getUUID());
        if (startTick == null) {
            return false;
        }

        long elapsed = hivemind_now(world) - startTick;
        return elapsed >= ACTIVE_DURATION_TICKS && elapsed < TOTAL_CYCLE_TICKS;
    }

    /*
     * Limpa registros antigos quando o ciclo completo já terminou.
     *
     * Comunicação:
     * - usado por hivemind_tryActivateForPlayer
     */
    private static void hivemind_cleanupExpiredCycle(ServerLevel world, Player player) {
        UUID playerId = player.getUUID();
        Long startTick = ACTIVE_START_TICK.get(playerId);
        if (startTick == null) {
            return;
        }

        long elapsed = hivemind_now(world) - startTick;
        if (elapsed >= TOTAL_CYCLE_TICKS) {
            ACTIVE_START_TICK.remove(playerId);
        }
    }

    /*
     * Tenta ativar ou continuar o hivemind para este jogador.
     *
     * Regras:
     * - se estiver ativo, continua normalmente
     * - se estiver em cooldown, bloqueia
     * - se o ciclo terminou, limpa e permite nova ativação
     * - se nunca ativou, inicia agora
     *
     * Comunicação:
     * - chamada no evento de dano antes de chamar hivemind_callNearby
     */
    public static boolean hivemind_tryActivateForPlayer(ServerLevel world, Player player) {
        if (hivemind_isWorldZ(world)) {
            return true;
        }

        if (!HIVEMIND_EVENT_ENABLED) {
            return false;
        }

        hivemind_cleanupExpiredCycle(world, player);

        if (hivemind_isPlayerActive(world, player)) {
            return true;
        }

        if (hivemind_isPlayerOnCooldown(world, player)) {
            return false;
        }

        ACTIVE_START_TICK.put(player.getUUID(), hivemind_now(world));

        world.playSound(
                null,
                player.blockPosition(),
                SoundEvents.RAID_HORN.value(),
                SoundSource.HOSTILE,
                1.0f,
                1.0f);

        return true;
    }

    // Esta função define mobs que ficam sempre fora do hivemind.
    // Comunicação:
    // - usada por hivemind_canJoinForPlayer
    // - NÃO exclui skeleton, stray, bogged, creeper, witch e wither skeleton,
    // pois esses agora dependem da progressão do player
    private static boolean hivemind_isAlwaysExcluded(LivingEntity entity) {
        return entity instanceof CaveSpider
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

        if (player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET))
            count++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE))
            count++;
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(Items.IRON_LEGGINGS))
            count++;
        if (player.getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS))
            count++;

        return count;
    }

    private static int hivemind_countDiamondArmor(Player player) {
        int count = 0;

        if (player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET))
            count++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE))
            count++;
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(Items.DIAMOND_LEGGINGS))
            count++;
        if (player.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS))
            count++;

        return count;
    }

    private static int hivemind_countNetheriteArmor(Player player) {
        int count = 0;

        if (player.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET))
            count++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE))
            count++;
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS))
            count++;
        if (player.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS))
            count++;

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
        if (hivemind_isWorldZ(player.level())) {
            return 128.0;
        }
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

        return 32.0;
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
        if (hivemind_isWorldZ(player.level())) {
            return 80;
        }
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
    // - full diamante: libera skeleton, stray, spider e bogged
    // - qualquer peça de netherite: mantém skeletons e libera creeper
    // - full netherite: libera witch e wither skeleton
    // Comunicação:
    // - usada tanto no gatilho inicial quanto na seleção dos mobs próximos
    public static boolean hivemind_canJoinForPlayer(LivingEntity entity, Player player) {
        if (!(entity instanceof Enemy)) {
            return false;
        }

        if (hivemind_isWorldZ(player.level())) {
            if (entity instanceof WitherSkeleton) {
                return false;
            }

            return !(entity instanceof Warden)
                    && !(entity instanceof WitherBoss)
                    && !(entity instanceof EnderDragon);
        }

        if (hivemind_isAlwaysExcluded(entity)) {
            return false;
        }

        boolean fullDiamond = hivemind_isFullDiamond(player);
        boolean anyNetherite = hivemind_hasAnyNetherite(player);
        boolean fullNetherite = hivemind_isFullNetherite(player);

        if (entity instanceof Skeleton || entity instanceof Stray || entity instanceof Bogged
                || entity instanceof Spider) {
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
        hivemind_registerCommands();
        // Este evento dispara o hivemind quando o player causa dano em um mob válido.
        // Comunicação:
        // - chama hivemind_canJoinForPlayer para validar o mob inicial
        // - chama hivemind_callNearby para propagar o ataque
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (LivingEntity entity, DamageSource source, float amount) -> {

                    Level w = entity.level();
                    if (!(w instanceof ServerLevel world))
                        return true;

                    ServerPlayer player;
                    LivingEntity triggerMob;

                    Entity attackerEntity = source.getEntity();

                    if (attackerEntity instanceof ServerPlayer attackingPlayer) {
                        player = attackingPlayer;
                        triggerMob = entity;
                    } else if (entity instanceof ServerPlayer damagedPlayer
                            && attackerEntity instanceof LivingEntity attackingMob) {
                        player = damagedPlayer;
                        triggerMob = attackingMob;
                    } else {
                        return true;
                    }

                    if (!hivemind_canJoinForPlayer(triggerMob, player))
                        return true;

                    if (!hivemind_tryActivateForPlayer(world, player))
                        return true;

                    hivemind_callNearby(world, triggerMob, player);

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
                                        new ItemStack(Items.EMERALD)));
                    }
                });

        /*
         * Atualiza o contador do Hivemind no action bar.
         *
         * Regras:
         * - roda no fim do tick do servidor
         * - envia só 1 vez por segundo
         * - mostra apenas para players que estão na janela ativa
         */
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long gameTime = server.overworld().getGameTime();

            if (gameTime % 20L != 0L) {
                return;
            }

            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    if (hivemind_isWorldZ(level)) {
                        hivemind_callNearby(level, player, player);
                        hivemind_trySpawnHorseman(level, player);
                        continue;
                    }

                    if (hivemind_isPlayerActive(level, player)) {
                        hivemind_sendActionBar(level, player);
                        hivemind_callNearby(level, player, player);
                        hivemind_trySpawnHorseman(level, player);
                    }
                }
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
            if (center instanceof Mob centerMob) {
                centerMob.setTarget(player);
            }

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

    private static void hivemind_trySpawnHorseman(
            ServerLevel level,
            ServerPlayer player) {

        if (!level.isThundering()) {
            return;
        }

        /*
         * MundoZ:
         * spawn mais agressivo.
         *
         * Overworld/Nether:
         * só durante evento HiveMind.
         */
        int chance = hivemind_isWorldZ(level)
                ? 1200
                : 6000;

        if (level.getRandom().nextInt(chance) != 0)
            return;

        double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = 24 + level.getRandom().nextInt(16);

        int x = (int) (player.getX() + Math.cos(angle) * distance);
        int z = (int) (player.getZ() + Math.sin(angle) * distance);

        int y = level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                x,
                z);

        BlockPos spawnPos = new BlockPos(x, y, z);

        /*
         * Evita spawn subterrâneo.
         */
        if (!level.canSeeSky(spawnPos)) {
            return;
        }

        SkeletonHorse horse = net.minecraft.world.entity.EntityTypes.SKELETON_HORSE.spawn(
                level,
                spawnPos,
                EntitySpawnReason.EVENT);

        Skeleton skeleton = net.minecraft.world.entity.EntityTypes.SKELETON.spawn(
                level,
                spawnPos.above(),
                EntitySpawnReason.EVENT);

        if (horse == null || skeleton == null) {
            return;
        }

        /*
         * Equipamento.
         */
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));

        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));

        /*
         * Passageiro.
         */
        skeleton.startRiding(horse);

        level.addFreshEntity(horse);
        level.addFreshEntity(skeleton);

        /*
         * Já nasce agressivo.
         */
        horse.setTarget(player);
        skeleton.setTarget(player);

        /*
         * Atmosfera.
         */
        var lightning = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.spawn(
                level,
                spawnPos,
                EntitySpawnReason.EVENT);

        if (lightning != null) {
            level.addFreshEntity(lightning);
        }
    }
}