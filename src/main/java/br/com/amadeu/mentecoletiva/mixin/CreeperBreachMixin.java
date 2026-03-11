package br.com.amadeu.mentecoletiva.mixin;

import br.com.amadeu.mentecoletiva.mixin.accessor.EntityCollisionAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(CreeperEntity.class)
public abstract class CreeperBreachMixin {

    // ===== Ajustes =====
    @Unique private static final int CHECK_EVERY_TICKS = 2;        // responsivo sem pesar
    @Unique private static final double MAX_TARGET_DIST = 24.0;    // distância máxima do "cerco"
    @Unique private static final int MEMORY_TICKS = 300;           // 15s de memória
    @Unique private static final int COOLDOWN_TICKS = 60;          // 3s
    @Unique private static final boolean WALLHACK_TARGET = true;   // mantém target através da parede

    // Anti-degrau:
    @Unique private static final int HITWALL_REQUIRED = 6;         // precisa "bater na parede" por vários checks
    @Unique private static final double FRONT_DISTANCE = 0.9;      // onde checar o bloco à frente
    // ===================

    @Unique private int hivemind_tickCounter = 0;
    @Unique private int hivemind_cooldown = 0;

    // Memória do alvo
    @Unique private UUID hivemind_memoryTarget = null;
    @Unique private int hivemind_memoryTicks = 0;

    // Contador de colisões consecutivas (anti degrau)
    @Unique private int hivemind_hitWallStreak = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void hivemind_tick(CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity)(Object)this;
        World world = creeper.getEntityWorld();
        if (world.isClient()) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        if (hivemind_cooldown > 0) hivemind_cooldown--;
        if (hivemind_memoryTicks > 0) hivemind_memoryTicks--;

        // Atualiza memória se atualmente está mirando um player
        LivingEntity currentTarget = creeper.getTarget();
        if (currentTarget instanceof PlayerEntity p) {
            hivemind_memoryTarget = p.getUuid();
            hivemind_memoryTicks = MEMORY_TICKS;
        }

        // Resolve player: target atual OU memória
        PlayerEntity player = null;
        if (currentTarget instanceof PlayerEntity p) {
            player = p;
        } else if (hivemind_memoryTarget != null && hivemind_memoryTicks > 0) {
            player = serverWorld.getPlayerByUuid(hivemind_memoryTarget);
        }

        if (player == null) {
            hivemind_hitWallStreak = 0;
            return;
        }

        // Mantém target mesmo sem visão (cerco)
        if (WALLHACK_TARGET && creeper.getTarget() != player) {
            creeper.setTarget(player);
        }

        // Checa a cada X ticks
        hivemind_tickCounter++;
        if (hivemind_tickCounter < CHECK_EVERY_TICKS) return;
        hivemind_tickCounter = 0;

        if (creeper.isIgnited()) return;

        if (creeper.distanceTo(player) > MAX_TARGET_DIST) {
            hivemind_hitWallStreak = 0;
            return;
        }

        if (hivemind_cooldown > 0) return;

        // 1) precisa estar colidindo horizontalmente
        boolean hitWall = ((EntityCollisionAccessor) creeper).hivemind_horizontalCollision();
        if (!hitWall) {
            hivemind_hitWallStreak = 0;
            return;
        }

        // 2) checa se é "parede de verdade" (bloco na frente e o bloco de cima também bloqueiam)
        if (!hivemind_isRealWallAhead(serverWorld, creeper)) {
            // Provavelmente degrau/stairs/quina -> não explode
            hivemind_hitWallStreak = 0;
            return;
        }

        // 3) precisa persistir por alguns checks (anti degrau)
        hivemind_hitWallStreak++;
        if (hivemind_hitWallStreak >= HITWALL_REQUIRED) {
            creeper.ignite();
            hivemind_cooldown = COOLDOWN_TICKS;
            hivemind_hitWallStreak = 0;
        }
    }

    @Unique
    private static boolean hivemind_isRealWallAhead(ServerWorld world, CreeperEntity creeper) {
        // Direção horizontal aproximada do olhar do creeper
        Vec3d look = creeper.getRotationVec(1.0F);
        Direction dir = Direction.getFacing(look.x, 0.0, look.z);
        if (dir == Direction.UP || dir == Direction.DOWN) return false;

        // Posição “na frente” ao nível do corpo (pé) e da cabeça
        Vec3d front = new Vec3d(
                creeper.getX() + dir.getOffsetX() * FRONT_DISTANCE,
                creeper.getY(),
                creeper.getZ() + dir.getOffsetZ() * FRONT_DISTANCE
        );

        BlockPos footPos = BlockPos.ofFloored(front.x, creeper.getY() + 0.1, front.z);
        BlockPos headPos = footPos.up(); // bloco acima

        BlockState foot = world.getBlockState(footPos);
        BlockState head = world.getBlockState(headPos);

        boolean footBlocks = !foot.getCollisionShape(world, footPos).isEmpty();
        boolean headBlocks = !head.getCollisionShape(world, headPos).isEmpty();

        // Parede = bloqueia embaixo e em cima
        return footBlocks && headBlocks;
    }
}
