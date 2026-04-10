package br.com.amadeu.mentecoletiva.mixin;

import br.com.amadeu.mentecoletiva.HiveMindFlag;
import br.com.amadeu.mentecoletiva.HiveMindMod;
import br.com.amadeu.mentecoletiva.SwarmRoleFlag;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AbstractSkeleton.class)
public abstract class SkeletonAimMixin {

    private static final float MULT = 0.01f;

    @ModifyArgs(
        method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileUsingShoot(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;DDDFF)Lnet/minecraft/world/entity/projectile/Projectile;"
        )
    )
    private void hivemind_reduceDivergence(Args args) {
        AbstractSkeleton skel = (AbstractSkeleton)(Object)this;

        if (!(skel instanceof HiveMindFlag flag)) return;
        if (flag.hivemind_getActiveTicks() <= 0) return;

        if (!(skel instanceof SwarmRoleFlag roleFlag)) return;
        if (roleFlag.mentecoletiva_getRole() != HiveMindMod.ROLE_BACKLINE) return;

        // último float = inaccuracy
        float inaccuracy = (float) args.get(7);

        args.set(7, inaccuracy * MULT);
    }
}
