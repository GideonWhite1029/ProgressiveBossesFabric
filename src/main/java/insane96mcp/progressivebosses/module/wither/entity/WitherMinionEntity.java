package insane96mcp.progressivebosses.module.wither.entity;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.monster.AbstractSkeletonEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class WitherMinionEntity extends AbstractSkeletonEntity {

	public WitherMinionEntity(EntityType<? extends AbstractSkeletonEntity> type, World worldIn) {
		super(type, worldIn);
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_STEP;
	}

	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT;
	}

	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return SoundEvents.ENTITY_WITHER_SKELETON_HURT;
	}

	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_DEATH;
	}

	protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
		return 1.3F;
	}

	/**
	 * Gives armor or weapon for entity based on given DifficultyInstance
	 */
	protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
		this.setItemStackToSlot(EquipmentSlotType.MAINHAND, ItemStack.EMPTY);
	}

	@Nullable
	public ILivingEntityData onInitialSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
		ILivingEntityData ilivingentitydata = super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0D);
		this.setCombatTask();
		return ilivingentitydata;
	}

	public boolean attackEntityAsMob(Entity entityIn) {
		if (!super.attackEntityAsMob(entityIn)) {
			return false;
		} else {
			if (entityIn instanceof LivingEntity) {
				((LivingEntity)entityIn).addPotionEffect(new EffectInstance(Effects.WITHER, 200));
			}

			return true;
		}
	}

	public boolean isPotionApplicable(EffectInstance potioneffectIn) {
		return potioneffectIn.getPotion() != Effects.WITHER && super.isPotionApplicable(potioneffectIn);
	}

	public static AttributeModifierMap.MutableAttribute prepareAttributes() {
		return LivingEntity.registerAttributes()
				.createMutableAttribute(Attributes.ATTACK_DAMAGE, 3.0d)
				.createMutableAttribute(Attributes.MAX_HEALTH, 20.0d)
				.createMutableAttribute(Attributes.FOLLOW_RANGE, 40.0d)
				.createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.25d)
				.createMutableAttribute(Attributes.ATTACK_KNOCKBACK, 1.5d);
	}
}
