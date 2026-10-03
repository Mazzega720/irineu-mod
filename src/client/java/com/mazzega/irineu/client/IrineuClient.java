package com.mazzega.irineu.client;

import com.mazzega.irineu.Irineu;
import com.mazzega.irineu.client.bambam.ShockwaveBlockRenderer;
import com.mazzega.irineu.client.bambam.ThrownTreeRenderer;
import com.mazzega.irineu.client.brasil.BichoGeoRenderer;
import com.mazzega.irineu.client.brasil.BotoRenderer;
import com.mazzega.irineu.client.brasil.HavaianaRenderer;
import com.mazzega.irineu.client.brasil.NpcGeoRenderer;
import com.mazzega.irineu.client.brasil.TatuBolaRenderer;
import com.mazzega.irineu.client.caneta.FlyingPenRenderer;
import com.mazzega.irineu.client.caneta.PenModels;
import com.mazzega.irineu.client.caneta.PenProjectileRenderer;
import com.mazzega.irineu.client.davi.DaviModel;
import com.mazzega.irineu.client.davi.DaviRenderer;
import com.mazzega.irineu.client.gecko.BamBamGeoRenderer;
import com.mazzega.irineu.client.chefao.ChefaoGeoRenderer;
import com.mazzega.irineu.client.chefao.EstrelaRenderer;
import com.mazzega.irineu.client.chefao.GadoGeoRenderer;
import com.mazzega.irineu.client.chefao.SuperMitadaRenderer;
import com.mazzega.irineu.client.desafio.BolaRenderer;
import com.mazzega.irineu.client.desafio.DesafioHud;
import com.mazzega.irineu.client.desafio.DesafioScreen;
import com.mazzega.irineu.client.economia.MaquininhaScreen;
import com.mazzega.irineu.client.gecko.ManoelGeoRenderer;
import com.mazzega.irineu.client.gecko.PersonGeoRenderer;
import com.mazzega.irineu.desafio.Desafio;
import com.mazzega.irineu.desafio.DesafioPayloads;
import com.mazzega.irineu.registry.BrasilEntities;
import com.mazzega.irineu.registry.BrasilMenus;
import com.mazzega.irineu.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class IrineuClient implements ClientModInitializer {
	public static final ModelLayerLocation HUMANOID_LAYER = new ModelLayerLocation(Irineu.id("humanoid"), "main");
	public static final ModelLayerLocation DAVI_LAYER = new ModelLayerLocation(Irineu.id("davi"), "main");
	public static final ModelLayerLocation PEN_LAYER = new ModelLayerLocation(Irineu.id("caneta"), "main");

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(HUMANOID_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64));
		ModelLayerRegistry.registerModelLayer(DAVI_LAYER, DaviModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(PEN_LAYER, PenModels::createLayer);
		EntityRenderers.register(ModEntities.IRINEU, context -> new SkinnedHumanoidRenderer<>(context, Irineu.id("textures/entity/irineu.png")));
		EntityRenderers.register(ModEntities.JAILSON, context -> new SkinnedHumanoidRenderer<>(context, Irineu.id("textures/entity/jailson.png")));
		// BamBam e Manoel Gomes: modelos e animações do GeckoLib.
		EntityRenderers.register(ModEntities.BAMBAM, BamBamGeoRenderer::new);
		EntityRenderers.register(ModEntities.THROWN_TREE, ThrownTreeRenderer::new);
		EntityRenderers.register(ModEntities.SHOCKWAVE_BLOCK, ShockwaveBlockRenderer::new);
		EntityRenderers.register(ModEntities.SEAT, NoopRenderer::new);
		EntityRenderers.register(ModEntities.DAVI, DaviRenderer::new);
		EntityRenderers.register(ModEntities.MANOEL_GOMES, ManoelGeoRenderer::boss);
		EntityRenderers.register(ModEntities.MANOEL_CLONE, ManoelGeoRenderer::clone);
		// Luva de Pedreiro, Allan Jesus e a bola das embaixadinhas
		ModelLayerRegistry.registerModelLayer(BolaRenderer.LAYER, BolaRenderer::createLayer);
		EntityRenderers.register(ModEntities.LUVA_DE_PEDREIRO, context -> new PersonGeoRenderer<>(context, ModEntities.LUVA_DE_PEDREIRO));
		EntityRenderers.register(ModEntities.ALLAN_JESUS, context -> new PersonGeoRenderer<>(context, ModEntities.ALLAN_JESUS));
		EntityRenderers.register(ModEntities.BOLA, BolaRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(DesafioPayloads.Oferta.TYPE, (payload, context) -> context.client().execute(() ->
			context.client().gui.setScreen(new DesafioScreen(payload.luvaId(), payload.allanId(), Desafio.byIndex(payload.desafio()), payload.premio()))));
		HudElementRegistry.addLast(Irineu.id("desafio_placar"), new DesafioHud());
		// Chefão final: Lula, Bolsonaro, Lulonaro (gigante), Padre Kelmon, gados e os projéteis
		EntityRenderers.register(ModEntities.LULA, context -> new ChefaoGeoRenderer<>(context, ModEntities.LULA, 1.0F));
		EntityRenderers.register(ModEntities.BOLSONARO, context -> new ChefaoGeoRenderer<>(context, ModEntities.BOLSONARO, 1.0F));
		EntityRenderers.register(ModEntities.LULONARO, context -> new ChefaoGeoRenderer<>(context, ModEntities.LULONARO, 2.45F));
		EntityRenderers.register(ModEntities.PADRE_KELMON, context -> new PersonGeoRenderer<>(context, ModEntities.PADRE_KELMON));
		EntityRenderers.register(ModEntities.GADO, GadoGeoRenderer::new);
		EntityRenderers.register(ModEntities.COMIDA_ARREMESSADA, ThrownItemRenderer::new);
		EntityRenderers.register(ModEntities.TIRO, context -> new ThrownItemRenderer<>(context, 0.75F, true));
		EntityRenderers.register(ModEntities.ESTRELA_VERMELHA, EstrelaRenderer::new);
		// Bichos do Brasil.
		EntityRenderers.register(BrasilEntities.TAMANDUA, context -> new BichoGeoRenderer<>(context, BrasilEntities.TAMANDUA, 0.6F));
		EntityRenderers.register(BrasilEntities.LOBO_GUARA, context -> new BichoGeoRenderer<>(context, BrasilEntities.LOBO_GUARA, 0.45F));
		EntityRenderers.register(BrasilEntities.EMA, context -> new BichoGeoRenderer<>(context, BrasilEntities.EMA, 0.5F));
		EntityRenderers.register(BrasilEntities.MICO_LEAO, context -> new BichoGeoRenderer<>(context, BrasilEntities.MICO_LEAO, 0.25F));
		EntityRenderers.register(BrasilEntities.TUCANO, context -> new BichoGeoRenderer<>(context, BrasilEntities.TUCANO, 0.25F));
		EntityRenderers.register(BrasilEntities.CARCARA, context -> new BichoGeoRenderer<>(context, BrasilEntities.CARCARA, 0.3F));
		EntityRenderers.register(BrasilEntities.CORUJA_BURAQUEIRA, context -> new BichoGeoRenderer<>(context, BrasilEntities.CORUJA_BURAQUEIRA, 0.25F));
		EntityRenderers.register(BrasilEntities.VEADO_CAMPEIRO, context -> new BichoGeoRenderer<>(context, BrasilEntities.VEADO_CAMPEIRO, 0.5F));
		EntityRenderers.register(BrasilEntities.CAPIVARA, context -> new BichoGeoRenderer<>(context, BrasilEntities.CAPIVARA, 0.55F));
		EntityRenderers.register(BrasilEntities.JACARE, context -> new BichoGeoRenderer<>(context, BrasilEntities.JACARE, 0.7F));
		EntityRenderers.register(BrasilEntities.TUIUIU, context -> new BichoGeoRenderer<>(context, BrasilEntities.TUIUIU, 0.4F));
		EntityRenderers.register(BrasilEntities.TATU_BOLA, TatuBolaRenderer::new);
		EntityRenderers.register(BrasilEntities.BOTO, BotoRenderer::new);
		// Gente das estruturas (GeckoLib) e a havaiana arremessada; a tela da maquininha Pix.
		EntityRenderers.register(BrasilEntities.DONO_DO_BUTECO, context -> new NpcGeoRenderer<>(context, BrasilEntities.DONO_DO_BUTECO));
		EntityRenderers.register(BrasilEntities.CAMELO, context -> new NpcGeoRenderer<>(context, BrasilEntities.CAMELO));
		EntityRenderers.register(BrasilEntities.MERCEARIA, context -> new NpcGeoRenderer<>(context, BrasilEntities.MERCEARIA));
		EntityRenderers.register(BrasilEntities.FERRO_VELHO, context -> new NpcGeoRenderer<>(context, BrasilEntities.FERRO_VELHO));
		EntityRenderers.register(BrasilEntities.PESCADOR, context -> new NpcGeoRenderer<>(context, BrasilEntities.PESCADOR));
		EntityRenderers.register(BrasilEntities.GAUCHO, context -> new NpcGeoRenderer<>(context, BrasilEntities.GAUCHO));
		EntityRenderers.register(BrasilEntities.CANGACEIRO, context -> new NpcGeoRenderer<>(context, BrasilEntities.CANGACEIRO));
		EntityRenderers.register(BrasilEntities.HAVAIANA, HavaianaRenderer::new);
		MenuScreens.register(BrasilMenus.MAQUININHA, MaquininhaScreen::new);
		EntityRenderers.register(ModEntities.SUPER_MITADA, SuperMitadaRenderer::new);
		EntityRenderers.register(ModEntities.CANETA_PROJETIL, PenProjectileRenderer::new);
		EntityRenderers.register(ModEntities.CANETA_VOADORA, FlyingPenRenderer::new);
	}
}
