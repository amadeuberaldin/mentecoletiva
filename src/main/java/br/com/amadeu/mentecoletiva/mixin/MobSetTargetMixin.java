package br.com.amadeu.mentecoletiva.mixin;

import br.com.amadeu.mentecoletiva.HiveMindMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import br.com.amadeu.mentecoletiva.HiveMindFlag;

@Mixin(MobEntity.class)
public abstract class MobSetTargetMixin {

    @Inject(method = "setTarget", at = @At("HEAD"))
    private void hivemind_onSetTarget(LivingEntity target, CallbackInfo ci) {
        MobEntity self = (MobEntity)(Object)this;

        // ✅ Se o HiveMind está propagando targets, não reaja (evita loop/crash)
        if (HiveMindMod.hivemind_isPropagating()) return;

        World w = self.getEntityWorld();
        if (!(w instanceof ServerWorld world)) return;

        if (!(self instanceof HostileEntity)) return;
        if (self instanceof EndermanEntity) return;

        if (!(target instanceof PlayerEntity player)) return;

        // ✅ Evita disparar se já está mirando nesse player
        if (self.getTarget() == target) return;

	if (self instanceof HiveMindFlag flag) {
  	    flag.hivemind_setActiveTicks(200);
	}

        HiveMindMod.hivemind_callNearby(world, self, player);
    }
}
