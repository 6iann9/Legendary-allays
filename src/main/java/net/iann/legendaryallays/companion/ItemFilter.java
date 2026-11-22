package net.iann.legendaryallays.companion;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Item-type allowlist shared by all of a player's companions. Empty allows all items. */
public final class ItemFilter {
    private final Set<ResourceLocation> items = new LinkedHashSet<>();

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && (items.isEmpty() || items.contains(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

    public boolean add(ItemStack stack) {
        return !stack.isEmpty() && items.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public void clear() { items.clear(); }
    public int size() { return items.size(); }

    public ListTag save() {
        ListTag result = new ListTag();
        items.forEach(item -> result.add(StringTag.valueOf(item.toString())));
        return result;
    }

    public static ItemFilter load(ListTag tag) {
        ItemFilter filter = new ItemFilter();
        for (Tag value : tag) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id != null) filter.items.add(id);
        }
        return filter;
    }
}
