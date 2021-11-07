package insane96mcp.progressivebosses.module.elderguardian.feature;

import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.progressivebosses.base.Strings;
import insane96mcp.progressivebosses.setup.Config;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.monster.ElderGuardianEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Label(name = "Health", description = "Bonus Health and Health regeneration.")
public class HealthFeature extends Feature {

	private final ForgeConfigSpec.ConfigValue<Double> bonusHealthConfig;
	private final ForgeConfigSpec.ConfigValue<Double> absorptionHealthConfig;
	private final ForgeConfigSpec.ConfigValue<Double> healthRegenConfig;

	public double bonusHealth = 0.5d;
	public double absorptionHealth = 40d;
	public double healthRegen = 0.5d;

	public HealthFeature(Module module) {
		super(Config.builder, module);
		this.pushConfig(Config.builder);
		this.bonusHealthConfig = Config.builder
				.comment("Increase Elder Guardians' Health by this percentage (1 = +100% health)")
				.defineInRange("Health Bonus per Difficulty", this.bonusHealth, 0.0, Double.MAX_VALUE);
		this.absorptionHealthConfig = Config.builder
				.comment("Adds absorption health to Elder Guradians (health that doesn't regen)")
				.defineInRange("Absorption Health", this.absorptionHealth, 0.0, Double.MAX_VALUE);
		this.healthRegenConfig = Config.builder
				.comment("Health Regen per second")
				.defineInRange("Health Regen", this.healthRegen, 0.0, Double.MAX_VALUE);
		Config.builder.pop();
	}

	@Override
	public void loadConfig() {
		super.loadConfig();
		this.bonusHealth = this.bonusHealthConfig.get();
		this.absorptionHealth = this.absorptionHealthConfig.get();
		this.healthRegen = this.healthRegenConfig.get();
	}

	@SubscribeEvent
	public void onSpawn(EntityJoinWorldEvent event) {
		if (event.getWorld().isRemote)
			return;

		if (!this.isEnabled())
			return;

		if (this.bonusHealth == 0d && this.absorptionHealth == 0d)
			return;

		if (!(event.getEntity() instanceof ElderGuardianEntity))
			return;

		ElderGuardianEntity elderGuardian = (ElderGuardianEntity) event.getEntity();
		CompoundNBT nbt = elderGuardian.getPersistentData();
		if (nbt.getBoolean(Strings.Tags.DIFFICULTY))
			return;

		nbt.putBoolean(Strings.Tags.DIFFICULTY, true);

		if (this.bonusHealth != 0d) {
			if (elderGuardian.getAttribute(Attributes.MAX_HEALTH).getModifier(Strings.AttributeModifiers.BONUS_HEALTH_UUID) != null)
				return;

			ModifiableAttributeInstance health = elderGuardian.getAttribute(Attributes.MAX_HEALTH);
			AttributeModifier modifier = new AttributeModifier(Strings.AttributeModifiers.BONUS_HEALTH_UUID, Strings.AttributeModifiers.BONUS_HEALTH, this.bonusHealth, AttributeModifier.Operation.MULTIPLY_BASE);
			health.applyPersistentModifier(modifier);
			elderGuardian.setHealth(elderGuardian.getMaxHealth());
		}

		if (this.absorptionHealth > 0d)
			elderGuardian.setAbsorptionAmount((float) this.absorptionHealth);
	}

	@SubscribeEvent
	public void onUpdate(LivingEvent.LivingUpdateEvent event) {
		if (event.getEntity().world.isRemote)
			return;

		if (!this.isEnabled())
			return;

		if (!(event.getEntity() instanceof ElderGuardianEntity))
			return;

		if (this.healthRegen == 0d)
			return;

		ElderGuardianEntity elderGuardian = (ElderGuardianEntity) event.getEntity();

		if (!elderGuardian.isAlive())
			return;

		// divided by 20 because is the health regen per second and here I need per tick
		float heal = (float) this.healthRegen / 20f;
		elderGuardian.heal(heal);
	}
}
