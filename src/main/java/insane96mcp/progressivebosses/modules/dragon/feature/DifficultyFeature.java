package insane96mcp.progressivebosses.modules.dragon.feature;

import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.progressivebosses.base.Strings;
import insane96mcp.progressivebosses.setup.Config;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

@Label(name = "Difficulty Settings", description = "How difficulty is handled for the Dragon")
public class DifficultyFeature extends Feature {

	private final ForgeConfigSpec.ConfigValue<Boolean> sumKilledDragonDifficultyConfig;
	private final ForgeConfigSpec.ConfigValue<Integer> maxDifficultyConfig;
	private final ForgeConfigSpec.ConfigValue<Integer> startingDifficultyConfig;

	public boolean sumKilledDragonDifficulty = false;
	public int maxDifficulty = 82;
	public int startingDifficulty = 0;

	public DifficultyFeature(Module module) {
		super(Config.builder, module);
		Config.builder.comment(this.getDescription()).push(this.getName());
		sumKilledDragonDifficultyConfig = Config.builder
				.comment("If true and there are more players around the Dragon, she will have his stats based on the sum of both players' difficulty. If false, the Dragon stats will be based on the average of the difficulty of the players around.")
				.define("Sum Killed Dragons Difficulty", sumKilledDragonDifficulty);
		maxDifficultyConfig = Config.builder
				.comment("The Maximum difficulty (times killed) reachable by Ender Dragon. By default is set to 82 because the Ender Dragon reaches the maximum amount of health (1024, handled by Minecraft. Some mods can increase this) after 82 Dragons killed.")
				.defineInRange("Max Difficulty", maxDifficulty, 1, Integer.MAX_VALUE);
		startingDifficultyConfig = Config.builder
				.comment("How much difficulty will players start with when joining a world?")
				.defineInRange("Starting Difficulty", startingDifficulty, 1, Integer.MAX_VALUE);
		Config.builder.pop();
	}

	@Override
	public void loadConfig() {
		super.loadConfig();
		sumKilledDragonDifficulty = sumKilledDragonDifficultyConfig.get();
		maxDifficulty = maxDifficultyConfig.get();
		startingDifficulty = startingDifficultyConfig.get();
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void setDifficulty(EntityJoinWorldEvent event) {
		if (!event.getWorld().getDimensionKey().getLocation().equals(DimensionType.THE_END.getLocation()))
			return;

		if (!(event.getEntity() instanceof EnderDragonEntity))
			return;

		EnderDragonEntity dragon = (EnderDragonEntity) event.getEntity();

		if (dragon.getFightManager() == null)
			return;

		CompoundNBT dragonTags = dragon.getPersistentData();
		if (dragonTags.contains(Strings.Tags.DIFFICULTY))
			return;

		int radius = 256;
		BlockPos pos1 = new BlockPos(-radius, -radius, -radius);
		BlockPos pos2 = new BlockPos(radius, radius, radius);
		AxisAlignedBB bb = new AxisAlignedBB(pos1, pos2);

		List<ServerPlayerEntity> players = event.getWorld().getEntitiesWithinAABB(ServerPlayerEntity.class, bb);
		if (players.size() == 0)
			return;

		int eggsToDrop = 0;

		float killedTotal = 0;
		for (ServerPlayerEntity player : players) {
			CompoundNBT playerTags = player.getPersistentData();
			int killedDragons = playerTags.getInt(Strings.Tags.KILLED_DRAGONS);
			boolean hasGotEgg = playerTags.getBoolean(Strings.Tags.HAS_GOT_EGG);
			if (killedDragons == 0 || hasGotEgg) {
				dragon.getFightManager().previouslyKilled = false;
				eggsToDrop++;
				playerTags.putBoolean(Strings.Tags.HAS_GOT_EGG, true);
			}
			killedTotal += killedDragons;
		}

		dragonTags.putInt(Strings.Tags.EGGS_TO_DROP, eggsToDrop);

		//If no players are found in the Main End Island, try to get the nearest player
		if (killedTotal == 0) {
			PlayerEntity nearestPlayer = event.getWorld().getClosestPlayer(dragon.getPosX(), dragon.getPosY(), dragon.getPosZ(), Double.MAX_VALUE, true);
			if (nearestPlayer instanceof ServerPlayerEntity) {
				ServerPlayerEntity player = (ServerPlayerEntity) nearestPlayer;
				CompoundNBT playerTags = player.getPersistentData();
				int killedDragons = playerTags.getInt(Strings.Tags.KILLED_DRAGONS);
				killedTotal += killedDragons;
				if (killedDragons < this.maxDifficulty)
					playerTags.putInt(Strings.Tags.KILLED_DRAGONS, killedDragons + 1);
			}
		}

		if (killedTotal == 0)
			return;

		if (!this.sumKilledDragonDifficulty)
			killedTotal /= players.size();

		dragonTags.putFloat(Strings.Tags.KILLED_DRAGONS, killedTotal);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void setPlayerData(EntityJoinWorldEvent event) {
		if (!(event.getEntity() instanceof ServerPlayerEntity))
			return;

		ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();

		CompoundNBT playerTags = player.getPersistentData();
		if (!playerTags.contains(Strings.Tags.KILLED_DRAGONS))
			playerTags.putInt(Strings.Tags.KILLED_DRAGONS, this.startingDifficulty);

		if (!playerTags.contains(Strings.Tags.HAS_GOT_EGG))
			playerTags.putBoolean(Strings.Tags.HAS_GOT_EGG, false);
	}
}