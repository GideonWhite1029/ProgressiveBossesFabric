package insane96mcp.progressivebosses.module.dragon.feature;

import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.progressivebosses.setup.Config;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.IPhase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Arrays;
import java.util.List;

@Label(name = "Resistances & Vulnerabilities", description = "Handles the Damage Resistances and Vulnerabilities")
public class ResistancesFeature extends Feature {

	private final ForgeConfigSpec.ConfigValue<Double> bonusMeleeDamageWhenNotSittingConfig;
	private final ForgeConfigSpec.ConfigValue<Double> damageRedutionWhenSittingConfig;
	private final ForgeConfigSpec.ConfigValue<Double> explosionDamageReductionConfig;

	public double bonusMeleeDamageWhenNotSitting = 1.00d;
	public double damageRedutionWhenSitting = 0.15d;
	public double explosionDamageReduction = 0.50d;

	public ResistancesFeature(Module module) {
		super(Config.builder, module);
		Config.builder.comment(this.getDescription()).push(this.getName());
		bonusMeleeDamageWhenNotSittingConfig = Config.builder
				.comment("Percentage Bonus Melee damage while the Ender Dragon is not at the center.")
				.defineInRange("Bonus Melee damage when not sitting", bonusMeleeDamageWhenNotSitting, 0d, Double.MAX_VALUE);
		damageRedutionWhenSittingConfig = Config.builder
				.comment("Melee Damage reduction while the Ender Dragon is at the center.")
				.defineInRange("Melee Damage reduction while at the center", damageRedutionWhenSitting, 0d, Double.MAX_VALUE);
		explosionDamageReductionConfig = Config.builder
				.comment("Damage reduction when hit by explosions.")
				.defineInRange("Explosion Damage reduction", explosionDamageReduction, 0d, Double.MAX_VALUE);
		Config.builder.pop();
	}

	@Override
	public void loadConfig() {
		super.loadConfig();
		this.bonusMeleeDamageWhenNotSitting = this.bonusMeleeDamageWhenNotSittingConfig.get();
		this.damageRedutionWhenSitting = this.damageRedutionWhenSittingConfig.get();
		this.explosionDamageReduction = this.explosionDamageReductionConfig.get();
	}

	@SubscribeEvent
	public void onDragonDamage(LivingDamageEvent event) {
		if (!this.isEnabled())
			return;

		if (!(event.getEntity() instanceof EnderDragonEntity))
			return;

		EnderDragonEntity dragon = (EnderDragonEntity) event.getEntity();

		bonusMeleeDamage(event, dragon);
		meleeDamageReduction(event, dragon);
		explosionDamageReduction(event, dragon);
	}

	private static final List<PhaseType<? extends IPhase>> sittingPhases = Arrays.asList(PhaseType.SITTING_SCANNING, PhaseType.SITTING_ATTACKING, PhaseType.SITTING_FLAMING, PhaseType.TAKEOFF);

	private void bonusMeleeDamage(LivingDamageEvent event, EnderDragonEntity dragon) {
		if (this.bonusMeleeDamageWhenNotSitting == 0d)
			return;

		if (!sittingPhases.contains(dragon.getPhaseManager().getCurrentPhase().getType()) && event.getSource().getImmediateSource() instanceof PlayerEntity) {
			event.setAmount((event.getAmount() * (float) (this.bonusMeleeDamageWhenNotSitting + 1)));
		}
	}

	private void meleeDamageReduction(LivingDamageEvent event, EnderDragonEntity dragon) {
		if (this.damageRedutionWhenSitting == 0d)
			return;

		if (sittingPhases.contains(dragon.getPhaseManager().getCurrentPhase().getType()) && event.getSource().getImmediateSource() instanceof PlayerEntity) {
			event.setAmount((event.getAmount() - (float) (event.getAmount() * this.damageRedutionWhenSitting)));
		}
	}

	private void explosionDamageReduction(LivingDamageEvent event, EnderDragonEntity dragon) {
		if (this.explosionDamageReduction == 0d)
			return;

		if (event.getSource().isExplosion()) {
			event.setAmount((event.getAmount() - (float) (event.getAmount() * this.explosionDamageReduction)));
		}
	}
}
