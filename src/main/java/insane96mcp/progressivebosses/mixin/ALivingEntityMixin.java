package insane96mcp.progressivebosses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingDeathEvent;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import insane96mcp.progressivebosses.utils.PlayerEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(LivingEntity.class)
public class ALivingEntityMixin {
    @Inject(at = @At("TAIL"), method = "onDeath")
    public void onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntityEvents.DEATH.invoker().interact(new OnLivingDeathEvent((LivingEntity) (Object) this, source));
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"))
    public float onLivingHurt(float amount, DamageSource source) {
        OnLivingHurtEvent event = new OnLivingHurtEvent((LivingEntity) (Object) this, source, amount);
        LivingEntityEvents.HURT.invoker().interact(event);
        return event.amount;
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity) {
            LivingEntityEvents.TICK.invoker().interact((LivingEntity) (Object) this);
            if ((Object) this instanceof PlayerEntity) {
                PlayerEntityEvents.TICK.invoker().interact((PlayerEntity) (Object) this);
            }
        }
    }
}