package net.iann.legendaryallays.client;

import net.iann.legendaryallays.LegendaryAllays;
import net.iann.legendaryallays.registry.ModEntities;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.iann.legendaryallays.client.wheel.CommandWheel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = LegendaryAllays.MOD_ID, dist = Dist.CLIENT)
public final class LegendaryAllaysClient {
    public LegendaryAllaysClient(IEventBus modEventBus) {
        modEventBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(ModEntities.BOUND_ALLAY.get(), AllayRenderer::new));
        CommandWheel wheel = new CommandWheel();
        NeoForge.EVENT_BUS.register(wheel);
        modEventBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(LegendaryAllays.MOD_ID, "command_wheel"), wheel::render));
    }
}
