package insane96mcp.progressivebosses.modules.wither.feature;

import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.insanelib.utils.RandomHelper;
import insane96mcp.progressivebosses.ai.WitherMinionHurtByTargetGoal;
import insane96mcp.progressivebosses.base.Strings;
import insane96mcp.progressivebosses.setup.Config;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.monster.WitherSkeletonEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@Label(name = "Minions", description = "Wither will spawn deadly (and tall) Minions")
public class MinionFeature extends Feature {

	private final ForgeConfigSpec.ConfigValue<Integer> minionAtDifficultyConfig;
	private final ForgeConfigSpec.ConfigValue<Integer> bonusMinionEveryDifficultyConfig;
	private final ForgeConfigSpec.ConfigValue<Integer> maxSpawnedConfig;
	private final ForgeConfigSpec.ConfigValue<Integer> maxAroundConfig;
	private final ForgeConfigSpec.ConfigValue<Integer> minCooldownConfig;
	private final ForgeConfigSpec.ConfigValue<Integer> maxCooldownConfig;
	private final ForgeConfigSpec.ConfigValue<Double> cooldownMultiplierBelowHalfHealthConfig;
	private final ForgeConfigSpec.ConfigValue<Double> bonusSpeedPerDifficultyConfig;
	private final ForgeConfigSpec.ConfigValue<Boolean> killMinionOnWitherDeathConfig;

	public int minionAtDifficulty = 1;
	public int bonusMinionEveryDifficulty = 2;
	public int maxSpawned = 8;
	public int maxAround = 16;
	public int minCooldown = 300;
	public int maxCooldown = 600;
	public double cooldownMultiplierBelowHalfHealth = 0.35d;
	public double bonusSpeedPerDifficulty = 1d;
	public boolean killMinionOnWitherDeath = true;

	public MinionFeature(Module module) {
		super(Config.builder, module);
		Config.builder.comment(this.getDescription()).push(this.getName());
		minionAtDifficultyConfig = Config.builder
				.comment("At which difficulty the Wither starts spawning Minions")
				.defineInRange("Minion at Difficulty", minionAtDifficulty, 0, Integer.MAX_VALUE);
		bonusMinionEveryDifficultyConfig = Config.builder
				.comment("As the Wither starts spawning Minions, every how much difficulty the Wither will spawn one more Minion")
				.defineInRange("Bonus Minion Every Difficulty", bonusMinionEveryDifficulty, 0, Integer.MAX_VALUE);
		maxSpawnedConfig = Config.builder
				.comment("Maximum Minions spawned by the Wither")
				.defineInRange("Max Minions Spawned", maxSpawned, 0, Integer.MAX_VALUE);
		maxAroundConfig = Config.builder
				.comment("Maximum amount of Minions that can be around the Wither in a 32 block radius. After this number is reached the Wither will stop spawning minions. Set to 0 to disable this check")
				.defineInRange("Max Minions Around", maxAround, 0, Integer.MAX_VALUE);
		minCooldownConfig = Config.builder
				.comment("Minimum ticks (20 ticks = 1 seconds) after Minions can spwan.")
				.defineInRange("Minimum Cooldown", minCooldown, 0, Integer.MAX_VALUE);
		maxCooldownConfig = Config.builder
				.comment("Maximum ticks (20 ticks = 1 seconds) after Minions can spwan.")
				.defineInRange("Maximum Cooldown", maxCooldown, 0, Integer.MAX_VALUE);
		cooldownMultiplierBelowHalfHealthConfig = Config.builder
				.comment("Min and Max cooldowns are multiplied by this value when the Wither drops below half health. Set to 1 to not change the cooldown when the wither's health drops below half.")
				.defineInRange("Cooldown Multiplier Below Half Health", cooldownMultiplierBelowHalfHealth, 0d, Double.MAX_VALUE);
		bonusSpeedPerDifficultyConfig = Config.builder
				.comment("Percentage bonus speed per difficulty.")
				.defineInRange("Bonus Movement Speed Per Difficulty", bonusSpeedPerDifficulty, 0d, Double.MAX_VALUE);
		killMinionOnWitherDeathConfig = Config.builder
				.comment("Wither Minions will die when the Wither that spawned them dies. (Note: only minions in a 128 blocks radius will be killed)")
				.define("Kill Minions on Wither Death", killMinionOnWitherDeath);
		Config.builder.pop();
	}

	@Override
	public void loadConfig() {
		super.loadConfig();
		this.minionAtDifficulty = this.minionAtDifficultyConfig.get();
		this.bonusMinionEveryDifficulty = this.bonusMinionEveryDifficultyConfig.get();
		this.maxSpawned = this.maxSpawnedConfig.get();
		this.maxAround = this.maxAroundConfig.get();
		this.minCooldown = this.minCooldownConfig.get();
		this.maxCooldown = this.maxCooldownConfig.get();
		this.cooldownMultiplierBelowHalfHealth = this.cooldownMultiplierBelowHalfHealthConfig.get();
		this.bonusSpeedPerDifficulty = this.bonusSpeedPerDifficultyConfig.get();
		this.killMinionOnWitherDeath = this.killMinionOnWitherDeathConfig.get();
	}

	@SubscribeEvent
	public void onWitherSpawn(EntityJoinWorldEvent event) {
		if (event.getWorld().isRemote)
			return;

		if (!this.isEnabled())
			return;

		if (!(event.getEntity() instanceof WitherEntity))
			return;

		WitherEntity wither = (WitherEntity) event.getEntity();

		CompoundNBT witherTags = wither.getPersistentData();

		int cooldown = RandomHelper.getInt(wither.world.rand, this.minCooldown, this.maxCooldown);
		witherTags.putInt(Strings.Tags.WITHER_MINION_COOLDOWN, cooldown);
	}

	@SubscribeEvent
	public void onSkellySpawn(EntityJoinWorldEvent event) {
		if (event.getWorld().isRemote)
			return;

		if (!this.isEnabled())
			return;

		if (!(event.getEntity() instanceof WitherSkeletonEntity))
			return;

		WitherSkeletonEntity witherSkeletonEntity = (WitherSkeletonEntity) event.getEntity();

		CompoundNBT tags = witherSkeletonEntity.getPersistentData();
		if (!tags.contains(Strings.Tags.WITHER_MINION))
			return;

		setMinionAI(witherSkeletonEntity);
	}

	@SubscribeEvent
	public void update(LivingEvent.LivingUpdateEvent event) {
		if (event.getEntity().world.isRemote)
			return;

		if (!this.isEnabled())
			return;

		if (!(event.getEntity() instanceof WitherEntity))
			return;

		World world = event.getEntity().world;

		WitherEntity wither = (WitherEntity) event.getEntity();
		CompoundNBT witherTags = wither.getPersistentData();

		float difficulty = witherTags.getFloat(Strings.Tags.DIFFICULTY);
		if (difficulty < this.minionAtDifficulty)
			return;

		if (wither.getHealth() <= 0)
			return;

		if (wither.getInvulTime() > 0)
			return;

		int cooldown = witherTags.getInt(Strings.Tags.WITHER_MINION_COOLDOWN);
		if (cooldown > 0) {
			witherTags.putInt(Strings.Tags.WITHER_MINION_COOLDOWN, cooldown - 1);
			return;
		}

		//If there is no player in a radius from the wither, don't spawn minions
		int radius = 32;
		BlockPos pos1 = wither.getPosition().add(-radius, -radius, -radius);
		BlockPos pos2 = wither.getPosition().add(radius, radius, radius);
		AxisAlignedBB bb = new AxisAlignedBB(pos1, pos2);
		List<ServerPlayerEntity> players = world.getEntitiesWithinAABB(ServerPlayerEntity.class, bb);

		if (players.isEmpty())
			return;

		List<WitherSkeletonEntity> minionsInAABB = world.getEntitiesWithinAABB(WitherSkeletonEntity.class, bb);
		int minionsCountInAABB = minionsInAABB.size();

		if (minionsCountInAABB >= this.maxAround)
			return;

		int minCooldown = this.minCooldown;
		if (wither.isCharged())
			minCooldown *= this.cooldownMultiplierBelowHalfHealth;
		int maxCooldown = this.maxCooldown;
		if (wither.isCharged())
			maxCooldown *= this.cooldownMultiplierBelowHalfHealth;

		cooldown = RandomHelper.getInt(world.rand, minCooldown, maxCooldown);
		witherTags.putInt(Strings.Tags.WITHER_MINION_COOLDOWN, cooldown - 1);

		int minionSpawnedCount = 0;
		for (int i = this.minionAtDifficulty; i <= difficulty; i += this.bonusMinionEveryDifficulty) {

			WitherSkeletonEntity witherSkeleton = new WitherSkeletonEntity(EntityType.WITHER_SKELETON, world);
			CompoundNBT minionTags = witherSkeleton.getPersistentData();
			minionTags.putBoolean(Strings.Tags.WITHER_MINION, true);

			minionTags.putBoolean("mobspropertiesrandomness:processed", true);
			//TODO Scaling health

			int x = 0, y = 0, z = 0;
			//Tries to spawn the Minion up to 5 times
			for (int t = 0; t < 5; t++) {
				x = (int) (wither.getPosX() + (RandomHelper.getInt(world.rand, -3, 3)));
				y = (int) (wither.getPosY() + 3);
				z = (int) (wither.getPositionVec().getZ() + (RandomHelper.getInt(world.rand, -3, 3)));

				y = getYSpawn(witherSkeleton, new BlockPos(x, y, z), world, 8);
				if (y != -1)
					break;
			}
			if (y <= 0)
				continue;

			witherSkeleton.setPosition(x + 0.5f, y + 0.5f, z + 0.5f);
			witherSkeleton.setCustomName(new TranslationTextComponent(Strings.Translatable.WITHER_MINION));
			witherSkeleton.deathLootTable = LootTables.EMPTY;
			witherSkeleton.experienceValue = 1;
			witherSkeleton.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.STONE_SWORD));
			witherSkeleton.setDropChance(EquipmentSlotType.MAINHAND, Float.MIN_VALUE);

			ModifiableAttributeInstance movementSpeed = witherSkeleton.getAttribute(Attributes.MOVEMENT_SPEED);
			double speedBonus = this.bonusSpeedPerDifficulty / 100d;
			AttributeModifier movementSpeedModifier = new AttributeModifier(Strings.AttributeModifiers.MOVEMENT_SPEED_BONUS_UUID, Strings.AttributeModifiers.MOVEMENT_SPEED_BONUS, speedBonus, AttributeModifier.Operation.MULTIPLY_BASE);
			movementSpeed.applyPersistentModifier(movementSpeedModifier);

			//No need since EntityJoinWorldEvent is triggered
			//setMinionAI(witherSkeleton);

			ListNBT minionsList = witherTags.getList(Strings.Tags.MINIONS, Constants.NBT.TAG_COMPOUND);
			CompoundNBT uuid = new CompoundNBT();
			uuid.putUniqueId("uuid", witherSkeleton.getUniqueID());
			minionsList.add(uuid);
			witherTags.put(Strings.Tags.MINIONS, minionsList);

			world.addEntity(witherSkeleton);

			minionSpawnedCount++;
			if (minionSpawnedCount >= this.maxSpawned)
				break;

			minionsCountInAABB++;
			if (minionsCountInAABB >= this.maxAround)
				break;
		}
	}

	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		if (!this.isEnabled())
			return;

		if (!this.killMinionOnWitherDeath)
			return;

		if (!(event.getEntity() instanceof WitherEntity))
			return;

		WitherEntity wither = (WitherEntity) event.getEntity();
		World world = wither.world;

		CompoundNBT tags = wither.getPersistentData();
		ListNBT minionsList = tags.getList(Strings.Tags.MINIONS, Constants.NBT.TAG_COMPOUND);

		AxisAlignedBB axisAlignedBB = new AxisAlignedBB(new BlockPos(wither.getPosition().add(-128, -128, -128)), wither.getPosition().add(128, 128, 128));
		List<WitherSkeletonEntity> witherSkeletons = world.getEntitiesWithinAABB(WitherSkeletonEntity.class, axisAlignedBB);
		for (int i = 0; i < minionsList.size(); i++) {
			UUID uuid = minionsList.getCompound(i).getUniqueId("uuid");

			for (WitherSkeletonEntity skeleton : witherSkeletons) {
				if (skeleton.getUniqueID().equals(uuid)) {
					skeleton.addPotionEffect(new EffectInstance(Effects.INSTANT_HEALTH, 10000, 0, false, false));
					break;
				}
			}
		}
	}

	/**
	 * Returns -1 when no spawn spots are found, otherwise the Y coord
	 * @param witherSkeletonEntity
	 * @param pos
	 * @param world
	 * @param minRelativeY
	 * @return
	 */
	private static int getYSpawn(WitherSkeletonEntity witherSkeletonEntity, BlockPos pos, World world, int minRelativeY) {
		int height = (int) Math.ceil(witherSkeletonEntity.getHeight());
		int fittingYPos = -1;
		for (int y = pos.getY(); y > pos.getY() - minRelativeY; y--) {
			boolean viable = true;
			BlockPos p = new BlockPos(pos.getX(), y, pos.getZ());
			for (int i = 0; i < height; i++) {
				if (world.getBlockState(p.up(i)).getMaterial().blocksMovement()) {
					viable = false;
					break;
				}
			}
			if (!viable)
				continue;
			fittingYPos = y;
			if (!world.getBlockState(p.down()).getMaterial().blocksMovement())
				continue;
			return y;
		}
		return fittingYPos;
	}

	private static final Predicate<LivingEntity> NOT_UNDEAD = livingEntity -> livingEntity != null && livingEntity.getCreatureAttribute() != CreatureAttribute.UNDEAD && livingEntity.attackable();

	private static void setMinionAI(WitherSkeletonEntity witherSkeletonEntity) {
		ArrayList<Goal> toRemove = new ArrayList<>();
		witherSkeletonEntity.goalSelector.goals.forEach(goal -> {
			if (goal.getGoal() instanceof FleeSunGoal)
				toRemove.add(goal.getGoal());

			if (goal.getGoal() instanceof RestrictSunGoal)
				toRemove.add(goal.getGoal());

			if (goal.getGoal() instanceof AvoidEntityGoal)
				toRemove.add(goal.getGoal());
		});

		for (Goal goal : toRemove) {
			witherSkeletonEntity.goalSelector.removeGoal(goal);
		}
		toRemove.clear();

		witherSkeletonEntity.targetSelector.goals.forEach(goal -> {
			if (goal.getGoal() instanceof NearestAttackableTargetGoal)
				toRemove.add(goal.getGoal());
			if (goal.getGoal() instanceof HurtByTargetGoal)
				toRemove.add(goal.getGoal());
		});

		for (Goal goal : toRemove) {
			witherSkeletonEntity.targetSelector.removeGoal(goal);
		}
		toRemove.clear();

		witherSkeletonEntity.targetSelector.addGoal(1, new WitherMinionHurtByTargetGoal(witherSkeletonEntity, WitherEntity.class));
		witherSkeletonEntity.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(witherSkeletonEntity, PlayerEntity.class, true));
		witherSkeletonEntity.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(witherSkeletonEntity, IronGolemEntity.class, true));
		witherSkeletonEntity.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(witherSkeletonEntity, MobEntity.class, 0, false, false, NOT_UNDEAD));
	}
}
