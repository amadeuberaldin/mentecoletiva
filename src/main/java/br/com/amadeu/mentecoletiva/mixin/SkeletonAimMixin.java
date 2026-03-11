package br.com.amadeu.mentecoletiva.mixin;

import br.com.amadeu.mentecoletiva.HiveMindFlag;
import br.com.amadeu.mentecoletiva.HiveMindMod;
import br.com.amadeu.mentecoletiva.SwarmRoleFlag;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AbstractSkeletonEntity.class)
public abstract class SkeletonAimMixin {

    private static final float MULT = 0.01f;

    @ModifyArgs(
        method = "shootAt(Lnet/minecraft/entity/LivingEntity;F)V",
        at = @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/entity/projectile/ProjectileEntity;spawnWithVelocity(" +
                "Lnet/minecraft/entity/projectile/ProjectileEntity;" +
                "Lnet/minecraft/server/world/ServerWorld;" +
                "Lnet/minecraft/item/ItemStack;" +
                "DDDFF)" +
                "Lnet/minecraft/entity/projectile/ProjectileEntity;"
        )
    )
    private void hivemind_reduceDivergence(Args args) {
        AbstractSkeletonEntity skel = (AbstractSkeletonEntity)(Object)this;

        if (!(skel instanceof HiveMindFlag flag)) return;
        if (flag.hivemind_getActiveTicks() <= 0) return;

        if (!(skel instanceof SwarmRoleFlag roleFlag)) return;
        if (roleFlag.mentecoletiva_getRole() != HiveMindMod.ROLE_BACKLINE) return;

        float divergence = (float) args.get(7);
        args.set(7, divergence * MULT);
    }
}
