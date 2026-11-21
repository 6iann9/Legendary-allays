package net.iann.legendaryallays.companion;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SafeReleasePosition {
    public static final double RANGE = 16;
    private SafeReleasePosition() {}

    public static Vec3 aimedPoint(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        return player.serverLevel().clip(new ClipContext(start, start.add(player.getLookAngle().scale(RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player)).getLocation();
    }

    public static Optional<Vec3> find(ServerPlayer player, Allay allay) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = aimedPoint(player);
        Vec3 direction = end.subtract(eye).normalize();
        double distance = eye.distanceTo(end);
        // Search from the aimed surface back toward the player. This avoids tunnelling
        // through walls and checks the whole body, not just the ray's endpoint.
        for (double d = distance; d >= 0.75; d -= 0.25) {
            Vec3 center = eye.add(direction.scale(d));
            Vec3 feet = center.add(0, -allay.getBbHeight() / 2.0, 0);
            allay.setPos(feet.x, feet.y, feet.z);
            AABB box = allay.getBoundingBox().inflate(0.05);
            if (box.minY < level.getMinBuildHeight() || box.maxY >= level.getMaxBuildHeight()) continue;
            if (!level.getWorldBorder().isWithinBounds(box)) continue;
            if (!level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ),
                    BlockPos.containing(box.maxX, box.maxY, box.maxZ))) continue;
            if (level.noCollision(allay, box) && !level.containsAnyLiquid(box)) return Optional.of(feet);
        }
        return Optional.empty();
    }
}
