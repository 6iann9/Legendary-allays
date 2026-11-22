package net.iann.legendaryallays.companion;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;
import net.iann.legendaryallays.entity.BoundAllay;
import net.iann.legendaryallays.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** A per-player FIFO of live drops. It never creates loot or force-loads chunks. */
public final class DropCollection {
    static final int DEPARTURE_INTERVAL = 2;
    private static final String OWNER = "legendaryallays:drop_owner";
    private static final String SESSION = "legendaryallays:drop_session";

    // Runtime only. A new login/server session always starts idle with no queue.
    static final class State {
        final UUID session = UUID.randomUUID();
        CollectionMode mode;
        State(CollectionMode mode) { this.mode = mode; }
        long nextDepartureTick;
        int nextCompanionSlot;
        final LinkedHashMap<UUID, ItemEntity> queue = new LinkedHashMap<>();
    }

    private DropCollection() {}

    public static void start(ServerPlayer player, CollectionMode mode) {
        var states = CompanionRoster.get(player.server).collectionStates;
        State current = states.get(player.getUUID());
        if (current != null && current.mode == mode) return;
        if (current != null && current.mode.dropped && mode.dropped) {
            // Switching between Dropped and Everywhere retains the priority queue.
            current.mode = mode;
            if (!mode.nearby) NearbyCollection.cancel(player);
            return;
        }
        idle(player);
        states.put(player.getUUID(), new State(mode));
    }

    public static void idle(ServerPlayer player) {
        NearbyCollection.cancel(player);
        State state = CompanionRoster.get(player.server).collectionStates.remove(player.getUUID());
        if (state == null) return;
        state.queue.values().forEach(DropCollection::clearTracking);
        state.queue.clear();
    }

    public static boolean hasLute(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.LUTE.get())) return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockDrops(BlockDropsEvent event) {
        if (event.getBreaker() instanceof ServerPlayer player) track(player, event.getDrops());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void livingDrops(LivingDropsEvent event) {
        // getEntity identifies the shooter for projectiles as well as melee attackers.
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getEntity() != player) {
            track(player, event.getDrops());
        }
    }

    private static void track(ServerPlayer player, Collection<ItemEntity> drops) {
        State state = CompanionRoster.get(player.server).collectionStates.get(player.getUUID());
        if (state == null || !state.mode.dropped || !player.isAlive() || player.isSpectator() || !hasLute(player)) return;
        for (ItemEntity item : drops) {
            if (!CompanionRoster.get(player.server).filter(player.getUUID()).matches(item.getItem())
                    || item.getPersistentData().getBoolean(CompanionCargo.EXCLUDED)) continue;
            if (item.getTarget() != null && !item.getTarget().equals(player.getUUID())) continue;
            item.getPersistentData().putUUID(OWNER, player.getUUID());
            item.getPersistentData().putUUID(SESSION, state.session);
            state.queue.putIfAbsent(item.getUUID(), item);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !(event.getLevel() instanceof ServerLevel)) return;
        State state = validState(item);
        // Only update references here: entity-join callbacks must not access chunks.
        if (state != null) state.queue.put(item.getUUID(), item);
    }

    public static void tick(ServerPlayer player) {
        State state = CompanionRoster.get(player.server).collectionStates.get(player.getUUID());
        if (state == null) return;
        // Expired, collected, unloaded or merged-away entities are not collectible.
        // A still-eligible unloaded item rejoins the queue when its chunk loads again.
        state.queue.values().removeIf(item -> !item.isAlive() || item.getItem().isEmpty()
                || !(item.level() instanceof ServerLevel level) || level.getEntity(item.getUUID()) != item);
        if (!player.isAlive() || player.isSpectator() || !hasLute(player)) {
            NearbyCollection.cancel(player);
            return;
        }
        NearbyCollection.validate(player);
        boolean droppedWaiting = hasPriorityDrops(player);
        if (droppedWaiting) NearbyCollection.cancel(player);
        long now = player.server.overworld().getGameTime();
        if (now < state.nextDepartureTick) return;
        CompanionRoster roster = CompanionRoster.get(player.server);
        var entries = roster.entries(player.getUUID());
        // Round-robin keeps the whole group participating when several slots are free.
        for (int offset = 0; offset < entries.size(); offset++) {
            int index = (state.nextCompanionSlot + offset) % entries.size();
            var entry = entries.get(index);
            BoundAllay allay = entry.entity;
            if (allay == null || allay.isRemoved() || allay.level() != player.level() || !allay.canCollectDrop()) continue;
            var iterator = state.queue.values().iterator();
            while (iterator.hasNext()) {
                ItemEntity item = iterator.next();
                if (!(item.level() instanceof ServerLevel level) || level.getEntity(item.getUUID()) != item) {
                    // Includes drops whose spawn was canceled after the drop event.
                    iterator.remove();
                    continue;
                }
                if (item.level() != player.level()) continue;
                if (validState(item) != state || item.getPersistentData().getBoolean(CompanionCargo.EXCLUDED)
                        || (item.getTarget() != null && !item.getTarget().equals(player.getUUID()))) {
                    iterator.remove();
                    continue;
                }
                if (allay.collectDrop(item)) {
                    if (!item.isAlive() || item.getItem().isEmpty()) iterator.remove();
                    entry.capture();
                    roster.setDirty();
                    state.nextDepartureTick = now + DEPARTURE_INTERVAL;
                    state.nextCompanionSlot = (index + 1) % entries.size();
                    return; // At most one departure per interval; remaining drops stay queued.
                }
            }
        }
        if (state.mode.nearby && !droppedWaiting) NearbyCollection.dispatch(player, state, entries, now);
    }

    public static void filterChanged(ServerPlayer player) {
        CompanionRoster roster = CompanionRoster.get(player.server);
        State state = roster.collectionStates.get(player.getUUID());
        if (state == null) return;
        state.queue.values().removeIf(item -> {
            if (roster.filter(player.getUUID()).matches(item.getItem())) return false;
            clearTracking(item);
            return true;
        });
    }

    public static boolean isQueued(ItemEntity item) { return validState(item) != null; }

    public static boolean hasPriorityDrops(ServerPlayer player) {
        State state = CompanionRoster.get(player.server).collectionStates.get(player.getUUID());
        return state != null && state.mode.dropped && state.queue.values().stream().anyMatch(item ->
                item.level() == player.level() && item.isAlive() && !item.getItem().isEmpty()
                && player.serverLevel().getEntity(item.getUUID()) == item
                && CompanionRoster.get(player.server).filter(player.getUUID()).matches(item.getItem())
                && !item.getPersistentData().getBoolean(CompanionCargo.EXCLUDED)
                && (item.getTarget() == null || item.getTarget().equals(player.getUUID())));
    }

    /** Queue ownership must not be lost by a merge into an unrelated dropped stack. */
    public static boolean mayMerge(ItemEntity a, ItemEntity b) {
        if (NearbyCollection.isReserved(a) || NearbyCollection.isReserved(b)) return false;
        if (a.getPersistentData().getBoolean(CompanionCargo.EXCLUDED)
                != b.getPersistentData().getBoolean(CompanionCargo.EXCLUDED)) return false;
        State first = validState(a);
        State second = validState(b);
        return Objects.equals(first, second);
    }

    private static State validState(ItemEntity item) {
        var tag = item.getPersistentData();
        if (!tag.hasUUID(OWNER) || !tag.hasUUID(SESSION)) return null;
        if (!(item.level() instanceof ServerLevel level)) return null;
        State state = CompanionRoster.get(level.getServer()).collectionStates.get(tag.getUUID(OWNER));
        if (state != null && state.mode.dropped && state.session.equals(tag.getUUID(SESSION))
                && CompanionRoster.get(level.getServer()).filter(tag.getUUID(OWNER)).matches(item.getItem())) return state;
        // Discard stale eligibility after Idle, logout or restart; ordinary loot remains.
        clearTracking(item);
        return null;
    }

    private static void clearTracking(ItemEntity item) {
        item.getPersistentData().remove(OWNER);
        item.getPersistentData().remove(SESSION);
    }
}
