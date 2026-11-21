package net.iann.legendaryallays;

import net.iann.legendaryallays.registry.ModItems;
import net.iann.legendaryallays.registry.ModEntities;
import net.iann.legendaryallays.companion.CompanionManager;
import net.iann.legendaryallays.companion.DropCollection;
import net.iann.legendaryallays.network.LuteCommandPayload;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Common entry point for registration and shared setup. */
@Mod(LegendaryAllays.MOD_ID)
public final class LegendaryAllays {
    public static final String MOD_ID = "legendaryallays";

    public LegendaryAllays(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(ModEntities::attributes);
        modEventBus.addListener(LuteCommandPayload::register);
        NeoForge.EVENT_BUS.register(CompanionManager.class);
        NeoForge.EVENT_BUS.register(DropCollection.class);
        modEventBus.addListener(LegendaryAllays::addCreativeItems);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.LUTE);
        }
    }
}
