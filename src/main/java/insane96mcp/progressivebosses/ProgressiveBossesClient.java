package insane96mcp.progressivebosses;

import insane96mcp.progressivebosses.module.dragon.entity.AreaEffectCloud3DRenderer;
import insane96mcp.progressivebosses.module.dragon.entity.LarvaRenderer;
import insane96mcp.progressivebosses.module.wither.entity.WitherMinionRenderer;
import insane96mcp.progressivebosses.network.PacketManagerClient;
import me.lortseam.completeconfig.gui.ConfigScreenBuilder;
import me.lortseam.completeconfig.gui.cloth.ClothConfigScreenBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;

import static insane96mcp.progressivebosses.ProgressiveBosses.MODID;

public class ProgressiveBossesClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ProgressiveBosses.WITHER_MINION, WitherMinionRenderer::new);
		EntityRendererRegistry.register(ProgressiveBosses.LARVA, LarvaRenderer::new);
		EntityRendererRegistry.register(ProgressiveBosses.AREA_EFFECT_CLOUD_3D, AreaEffectCloud3DRenderer::new);
		PacketManagerClient.init();
		
//		if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
//			ConfigScreenBuilder.setMain(MODID, new ClothConfigScreenBuilder());
//		}
	}

}