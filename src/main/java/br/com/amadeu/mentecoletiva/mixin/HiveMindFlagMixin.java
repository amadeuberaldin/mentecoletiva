package br.com.amadeu.mentecoletiva.mixin;

import br.com.amadeu.mentecoletiva.HiveMindFlag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class HiveMindFlagMixin implements HiveMindFlag {

    @Unique private int hivemind_activeTicks = 0;

    @Override
    public void hivemind_setActiveTicks(int ticks) {
        // mantém o maior (pra não encurtar por acidente)
        if (ticks > hivemind_activeTicks) hivemind_activeTicks = ticks;
    }

    @Override
    public int hivemind_getActiveTicks() {
        return hivemind_activeTicks;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void hivemind_tickDown(CallbackInfo ci) {
        Mob self = (Mob)(Object)this;
        Level w = self.level();
        if (w.isClientSide()) return;

        if (hivemind_activeTicks > 0) hivemind_activeTicks--;
    }
}
