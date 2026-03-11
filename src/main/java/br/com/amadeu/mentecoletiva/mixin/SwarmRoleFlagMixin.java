package br.com.amadeu.mentecoletiva.mixin;

import br.com.amadeu.mentecoletiva.SwarmRoleFlag;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MobEntity.class)
public abstract class SwarmRoleFlagMixin implements SwarmRoleFlag {

    @Unique private int mentecoletiva_role = 0;

    @Override
    public void mentecoletiva_setRole(int role) {
        this.mentecoletiva_role = role;
    }

    @Override
    public int mentecoletiva_getRole() {
        return mentecoletiva_role;
    }
}
