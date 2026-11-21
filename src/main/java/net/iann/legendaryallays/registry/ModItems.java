package net.iann.legendaryallays.registry;

import net.iann.legendaryallays.LegendaryAllays;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LegendaryAllays.MOD_ID);
    public static final DeferredItem<Item> LUTE = ITEMS.registerSimpleItem("lute", new Item.Properties().stacksTo(1));

    private ModItems() {
    }
}
