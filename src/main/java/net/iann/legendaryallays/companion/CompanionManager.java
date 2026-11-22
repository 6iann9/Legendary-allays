package net.iann.legendaryallays.companion;

import java.util.Comparator;
import java.util.List;
import net.iann.legendaryallays.entity.BoundAllay;
import net.iann.legendaryallays.command.LuteCommand;
import net.iann.legendaryallays.command.LuteSounds;
import net.iann.legendaryallays.registry.ModEntities;
import net.iann.legendaryallays.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class CompanionManager {
    private CompanionManager() {}

    public static void command(ServerPlayer player, LuteCommand command) {
        if (!player.isAlive() || player.isSpectator() || !player.getMainHandItem().is(ModItems.LUTE.get())) return;
        if (player.getCooldowns().isOnCooldown(ModItems.LUTE.get())) return;
        player.getCooldowns().addCooldown(ModItems.LUTE.get(), 8);
        ensureCompanions(player);
        switch (command) {
            case BIND -> bind(player);
            case UNBIND -> unbind(player);
            case FILTER_ADD -> FilterCommands.addOffhand(player);
            case FILTER_CLEAR -> FilterCommands.clear(player);
            case COLLECT_DROPPED -> {
                DropCollection.start(player, CollectionMode.DROPPED);
                message(player, "collect_dropped");
                LuteSounds.play(player, LuteCommand.COLLECT_DROPPED);
            }
            case COLLECT_NEARBY -> {
                DropCollection.start(player, CollectionMode.NEARBY);
                message(player, "collect_nearby");
                LuteSounds.play(player, LuteCommand.COLLECT_NEARBY);
            }
            case COLLECT_EVERYWHERE -> {
                DropCollection.start(player, CollectionMode.EVERYWHERE);
                message(player, "collect_everywhere");
                LuteSounds.play(player, LuteCommand.COLLECT_EVERYWHERE);
            }
            case IDLE -> {
                DropCollection.idle(player);
                message(player, "idle");
                LuteSounds.play(player, LuteCommand.IDLE);
            }
        }
    }

    private static void bind(ServerPlayer player) {
        CompanionRoster roster = CompanionRoster.get(player.server);
        List<CompanionRoster.Entry> entries = roster.entries(player.getUUID());
        if (entries.size() >= CompanionRoster.LIMIT) { message(player, "limit"); return; }
        Vec3 start = player.getEyePosition();
        Vec3 end = SafeReleasePosition.aimedPoint(player);
        Allay target = null;
        double closest = start.distanceToSqr(end);
        for (Allay allay : player.serverLevel().getEntitiesOfClass(Allay.class,
                player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                a -> a.getType() == EntityType.ALLAY && a.isAlive())) {
            var hit = allay.getBoundingBox().inflate(0.15).clip(start, end);
            if (hit.isPresent() && hit.get().distanceToSqr(start) < closest) {
                target = allay;
                closest = hit.get().distanceToSqr(start);
            }
        }
        if (target == null) { message(player, "no_target"); return; }
        if (target.isLeashed() || target.isPassenger() || target.isVehicle()) { message(player, "attached"); return; }
        var liked = target.getBrain().getMemory(MemoryModuleType.LIKED_PLAYER);
        if (liked.isPresent() && !liked.get().equals(player.getUUID())) { message(player, "other_owner"); return; }
        CompoundTag data = target.saveWithoutId(new CompoundTag());
        CompanionRoster.Entry entry = new CompanionRoster.Entry(data);
        BoundAllay bound = create(player, entry, entries.size(), target.position());
        if (bound == null) { message(player, "failed"); return; }
        entry.entity = bound;
        entry.capture();
        entries.add(entry);
        target.discard();
        roster.setDirty();
        message(player, "bound");
        LuteSounds.play(player, LuteCommand.BIND);
    }

    private static void unbind(ServerPlayer player) {
        CompanionRoster roster = CompanionRoster.get(player.server);
        List<CompanionRoster.Entry> entries = roster.entries(player.getUUID());
        var choice = entries.stream().filter(e -> e.entity != null && !e.entity.isRemoved())
                .min(Comparator.comparing(e -> e.entity.hasCargo()));
        if (choice.isEmpty()) { message(player, "none_bound"); return; }
        CompanionRoster.Entry entry = choice.get();
        Allay normal = EntityType.ALLAY.create(player.serverLevel());
        if (normal == null) { message(player, "failed"); return; }
        normal.load(entry.entity.snapshot());
        normal.setDeltaMovement(Vec3.ZERO);
        var position = SafeReleasePosition.find(player, normal);
        if (position.isEmpty()) { message(player, "no_space"); return; }
        // First add the replacement; a canceled spawn leaves the bound companion intact.
        if (!player.serverLevel().addFreshEntity(normal)) { message(player, "failed"); return; }
        // Drop from the replacement, so failed item spawns retain their stack on the Allay.
        CompanionCargo.dropAll(normal, normal.position());
        NearbyCollection.release(entry.entity);
        entry.entity.discard();
        entries.remove(entry);
        roster.setDirty();
        message(player, "unbound");
        LuteSounds.play(player, LuteCommand.UNBIND);
    }

    private static BoundAllay create(ServerPlayer player, CompanionRoster.Entry entry, int slot, Vec3 position) {
        BoundAllay entity = ModEntities.BOUND_ALLAY.get().create(player.serverLevel());
        if (entity == null) return null;
        entity.initialize(entry.data, player.getUUID(), slot);
        entity.setPos(position.x, position.y, position.z);
        entity.setDeltaMovement(Vec3.ZERO);
        return player.serverLevel().addFreshEntity(entity) ? entity : null;
    }

    private static void ensureCompanions(ServerPlayer player) {
        CompanionRoster roster = CompanionRoster.get(player.server);
        List<CompanionRoster.Entry> entries = roster.entries(player.getUUID());
        for (int slot = 0; slot < entries.size(); slot++) {
            var entry = entries.get(slot);
            BoundAllay entity = entry.entity;
            if (entity != null && (entity.isRemoved() || entity.level() != player.level() || !player.isAlive())) {
                NearbyCollection.release(entity);
                entry.capture();
                entity.discard();
                entry.entity = null;
            }
            if (entry.entity == null && player.isAlive()) {
                Vec3 behind = player.position().subtract(player.getLookAngle().multiply(1, 0, 1).scale(2)).add(0, 1.2, 0);
                entry.entity = create(player, entry, slot, behind);
            }
            if (entry.entity != null) entry.entity.setFormationSlot(slot);
        }
        if (!entries.isEmpty()) roster.setDirty();
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.tickCount % 10 == 0) ensureCompanions(player);
            DropCollection.tick(player);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DropCollection.idle(player);
        CompanionRoster roster = CompanionRoster.get(player.server);
        for (var entry : roster.entries(player.getUUID())) {
            entry.capture();
            // Use the player's current dimension even during a death or portal transition.
            // This temporary object is never added to the world.
            BoundAllay cargo = ModEntities.BOUND_ALLAY.get().create(player.serverLevel());
            if (cargo != null) {
                cargo.initialize(entry.data, player.getUUID(), 0);
                cargo.dropCargo(player.position().add(0, 0.5, 0));
                entry.data = cargo.snapshot();
            }
            if (entry.entity != null) entry.entity.discard();
            entry.entity = null;
        }
        roster.setDirty();
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.legendaryallays." + key), true);
    }
}
