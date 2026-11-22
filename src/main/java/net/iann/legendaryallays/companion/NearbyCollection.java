package net.iann.legendaryallays.companion;

import java.util.Comparator;
import java.util.List;
import net.iann.legendaryallays.entity.BoundAllay;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;

/** Loaded-world scans and exclusive claims for fly-out/fly-back collection. */
public final class NearbyCollection {
    public static final double RANGE = 16;

    private NearbyCollection() {}

    public static boolean eligible(ServerPlayer player, ItemEntity item) {
        return item.level() == player.level() && item.isAlive() && !item.getItem().isEmpty()
                && player.serverLevel().getEntity(item.getUUID()) == item
                && item.distanceToSqr(player) <= RANGE * RANGE && !item.hasPickUpDelay()
                && CompanionRoster.get(player.server).filter(player.getUUID()).matches(item.getItem())
                && !item.getPersistentData().getBoolean(CompanionCargo.EXCLUDED)
                && (item.getTarget() == null || item.getTarget().equals(player.getUUID()))
                && !DropCollection.isQueued(item);
    }

    public static boolean canPursue(ServerPlayer player, BoundAllay allay, ItemEntity item) {
        var state = CompanionRoster.get(player.server).collectionStates.get(player.getUUID());
        return state != null && state.mode.nearby && player.isAlive() && !player.isSpectator()
                && DropCollection.hasLute(player) && !DropCollection.hasPriorityDrops(player)
                && allay.level() == player.level()
                && !allay.hasCargo() && eligible(player, item) && reservation(item) == allay;
    }

    public static void validate(ServerPlayer player) {
        for (var entry : CompanionRoster.get(player.server).entries(player.getUUID())) {
            BoundAllay allay = entry.entity;
            if (allay != null && allay.nearbyTarget() != null
                    && !canPursue(player, allay, allay.nearbyTarget())) release(allay);
        }
    }

    public static void cancel(ServerPlayer player) {
        for (var entry : CompanionRoster.get(player.server).entries(player.getUUID())) {
            if (entry.entity != null) release(entry.entity);
        }
        CompanionRoster.get(player.server).nearbyReservations.values().removeIf(allay -> allay.isRemoved()
                || allay.nearbyTarget() == null || player.getUUID().equals(allay.ownerId()));
    }

    public static void release(BoundAllay allay) {
        ItemEntity item = allay.nearbyTarget();
        if (item != null && allay.level() instanceof ServerLevel level) {
            CompanionRoster.get(level.getServer()).nearbyReservations.remove(item.getUUID(), allay);
        }
        allay.setNearbyTarget(null);
    }

    private static BoundAllay reservation(ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level)) return null;
        var claims = CompanionRoster.get(level.getServer()).nearbyReservations;
        BoundAllay allay = claims.get(item.getUUID());
        if (allay != null && (allay.isRemoved() || allay.level() != item.level() || allay.nearbyTarget() != item)) {
            claims.remove(item.getUUID());
            return null;
        }
        return allay;
    }

    /** Do not let a claimed stack merge away while its assigned Allay is approaching. */
    public static boolean isReserved(ItemEntity item) { return reservation(item) != null; }

    public static boolean dispatch(ServerPlayer player, DropCollection.State state,
            List<CompanionRoster.Entry> entries, long now) {
        List<ItemEntity> candidates = null;
        for (int offset = 0; offset < entries.size(); offset++) {
            int index = (state.nextCompanionSlot + offset) % entries.size();
            BoundAllay allay = entries.get(index).entity;
            if (allay == null || allay.isRemoved() || allay.level() != player.level()
                    || !allay.canCollectDrop() || allay.nearbyTarget() != null) continue;
            if (candidates == null) {
                candidates = player.serverLevel().getEntitiesOfClass(ItemEntity.class,
                        player.getBoundingBox().inflate(RANGE), item -> eligible(player, item) && !isReserved(item));
            }
            var choice = candidates.stream().min(Comparator.comparingDouble(item -> item.distanceToSqr(allay)));
            if (choice.isEmpty()) return false;
            ItemEntity item = choice.get();
            CompanionRoster.get(player.server).nearbyReservations.put(item.getUUID(), allay);
            allay.setNearbyTarget(item);
            state.nextCompanionSlot = (index + 1) % entries.size();
            state.nextDepartureTick = now + DropCollection.DEPARTURE_INTERVAL;
            return true;
        }
        return false;
    }
}
