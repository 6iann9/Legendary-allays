package net.iann.legendaryallays.companion;

import java.util.*;
import net.iann.legendaryallays.entity.BoundAllay;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Single persistent source of ownership across dimensions, death, and logout. */
public final class CompanionRoster extends SavedData {
    public static final int LIMIT = 8;
    private final Map<UUID, List<Entry>> owners = new HashMap<>();
    final Map<UUID, DropCollection.State> collectionStates = new HashMap<>();

    public static final class Entry {
        public CompoundTag data;
        public BoundAllay entity;
        public Entry(CompoundTag data) { this.data = data.copy(); }
        public void capture() { if (entity != null) data = entity.snapshot(); }
    }

    public static CompanionRoster get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanionRoster::new, CompanionRoster::load), "legendaryallays_companions");
    }

    public List<Entry> entries(UUID owner) { return owners.computeIfAbsent(owner, key -> new ArrayList<>()); }

    private static CompanionRoster load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanionRoster roster = new CompanionRoster();
        for (Tag value : tag.getList("Owners", Tag.TAG_COMPOUND)) {
            CompoundTag owner = (CompoundTag) value;
            if (!owner.hasUUID("Owner")) continue;
            List<Entry> entries = roster.entries(owner.getUUID("Owner"));
            for (Tag allay : owner.getList("Allays", Tag.TAG_COMPOUND)) entries.add(new Entry((CompoundTag) allay));
        }
        return roster;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag savedOwners = new ListTag();
        owners.forEach((id, entries) -> {
            if (entries.isEmpty()) return;
            CompoundTag owner = new CompoundTag();
            owner.putUUID("Owner", id);
            ListTag allays = new ListTag();
            for (Entry entry : entries) {
                entry.capture();
                allays.add(entry.data.copy());
            }
            owner.put("Allays", allays);
            savedOwners.add(owner);
        });
        tag.put("Owners", savedOwners);
        return tag;
    }
}
