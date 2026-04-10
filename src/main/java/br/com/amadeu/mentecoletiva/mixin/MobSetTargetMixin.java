package br.com.amadeu.mentecoletiva.mixin;

import br.com.amadeu.mentecoletiva.HiveMindFlag;
import br.com.amadeu.mentecoletiva.HiveMindMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobSetTargetMixin {

    @Inject(method = "setTarget", at = @At("HEAD"))
    private void hivemind_onSetTarget(LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob)(Object)this;

        if (HiveMindMod.hivemind_isPropagating()) return;

        Level w = self.level();
        if (!(w instanceof ServerLevel world)) return;

        if (!(self instanceof Monster)) return;
        if (self instanceof EnderMan) return;
        if (self instanceof ZombifiedPiglin) return;
        if (self instanceof Warden) return;
        if (self instanceof WitherBoss) return;

        if (!(target instanceof Player player)) return;

        if (self.getTarget() == target) return;

        if (self instanceof HiveMindFlag flag) {
            flag.hivemind_setActiveTicks(200);
        }

        HiveMindMod.hivemind_assignRole(self);
        HiveMindMod.hivemind_callNearby(world, self, player);
    }
}
