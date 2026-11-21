package net.iann.legendaryallays.entity;

import java.util.UUID;
import net.iann.legendaryallays.companion.CompanionCargo;
import net.iann.legendaryallays.companion.CompanionRoster;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Physical representation of an owned companion; the roster owns its saved data. */
public final class BoundAllay extends Allay {
    private static final double FOLLOW_SPEED = 0.6;
    private static final double DELIVERY_SPEED = 0.3;
    private static final float TURN_SPEED = 12.0F;

    private UUID owner;
    private int formationSlot;
    private int nextPickupTick;
    private int deliveryReadyTick;

    public BoundAllay(EntityType<? extends Allay> type, Level level) {
        super(type, level);
        makeCompanion();
    }

    public void initialize(CompoundTag vanillaData, UUID owner, int slot) {
        CompoundTag copy = vanillaData.copy();
        copy.remove("UUID");
        copy.remove("Leash");
        copy.remove("Passengers");
        load(copy);
        this.owner = owner;
        this.formationSlot = slot;
        makeCompanion();
    }

    public void setFormationSlot(int slot) { this.formationSlot = slot; }

    private void makeCompanion() {
        setNoAi(true);
        setNoGravity(true);
        setInvulnerable(true);
        setPersistenceRequired();
        noPhysics = true;
    }

    /** Produces vanilla-compatible data without the bound-only protection flags. */
    public CompoundTag snapshot() {
        CompoundTag data = saveWithoutId(new CompoundTag());
        data.remove("UUID");
        data.remove("Brain");
        data.remove("Leash");
        data.remove("Passengers");
        data.putBoolean("NoAI", false);
        data.putBoolean("NoGravity", false);
        data.putBoolean("Invulnerable", false);
        data.putShort("Fire", (short) -1);
        data.putFloat("FallDistance", 0);
        return data;
    }

    public boolean hasCargo() { return CompanionCargo.hasCargo(this); }

    public boolean canCollectDrop() {
        return !hasCargo() && tickCount >= nextPickupTick;
    }

    public void dropCargo(Vec3 position) { CompanionCargo.dropAll(this, position); }

    /** Single server-thread transfer: never copy a stack without removing it from the drop. */
    public boolean collectDrop(ItemEntity item) {
        if (level().isClientSide || !canCollectDrop() || !item.isAlive() || item.getItem().isEmpty()) return false;
        ItemStack source = item.getItem();
        int count = Math.min(source.getCount(), source.getMaxStackSize());
        ItemStack carried = source.copyWithCount(count);
        ItemStack remainder = source.copy();
        remainder.shrink(count);
        teleportTo(item.getX(), item.getY() + 0.25, item.getZ());
        setDeltaMovement(Vec3.ZERO);
        setItemSlot(EquipmentSlot.MAINHAND, carried);
        take(item, count);
        if (remainder.isEmpty()) item.discard();
        else item.setItem(remainder);
        deliveryReadyTick = tickCount + 4;
        return true;
    }

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide && owner != null) {
            Player player = level().getPlayerByUUID(owner);
            if (player instanceof ServerPlayer && player.isAlive()) {
                double yaw = Math.toRadians(player.getYRot());
                Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
                Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
                double side = (formationSlot % 2 == 0 ? -1 : 1) * (0.9 + (formationSlot / 2) * 0.45);
                double back = 1.8 + (formationSlot / 2) * 0.55;
                boolean delivering = hasCargo();
                Vec3 target = delivering ? player.position().add(0, 1.0, 0)
                        : player.position().subtract(forward.scale(back)).add(right.scale(side))
                            .add(0, 1.25 + Math.sin((tickCount + formationSlot * 15) * 0.07) * 0.12, 0);
                Vec3 difference = target.subtract(position());
                double speed = delivering ? DELIVERY_SPEED : FOLLOW_SPEED;
                Vec3 step = difference.scale(delivering ? 0.08 : 0.16);
                if (step.lengthSqr() > speed * speed) step = step.normalize().scale(speed);
                Vec3 next = !delivering && difference.lengthSqr() > 1024 ? target : position().add(step);
                setPos(next.x, next.y, next.z);
                // Face the route while travelling; ignore small hover corrections at rest.
                double horizontalDistance = difference.horizontalDistance();
                float desiredYaw = horizontalDistance > 0.2
                        ? (float) Math.toDegrees(Math.atan2(-difference.x, difference.z))
                        : player.getYRot();
                float desiredPitch = difference.lengthSqr() > 0.16
                        ? Mth.clamp((float) -Math.toDegrees(Math.atan2(difference.y, horizontalDistance)), -35.0F, 35.0F)
                        : 0.0F;
                setYRot(Mth.approachDegrees(getYRot(), desiredYaw, TURN_SPEED));
                setXRot(Mth.approachDegrees(getXRot(), desiredPitch, TURN_SPEED));
                setYHeadRot(getYRot());
                yBodyRot = getYRot();
                // Delivery continues in Idle and without a lute; only new pickups pause.
                if (delivering && tickCount >= deliveryReadyTick && position().distanceToSqr(target) <= 2.25) {
                    CompanionCargo.deliver(this, (ServerPlayer) player);
                    nextPickupTick = tickCount + 4;
                    deliveryReadyTick = tickCount + 20;
                    CompanionRoster.get(((ServerPlayer) player).server).setDirty();
                }
            }
        }
    }

    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean canCollideWith(Entity entity) { return false; }
    @Override public void push(Entity entity) {}
    @Override public void push(double x, double y, double z) {}
    @Override public boolean canPickUpLoot() { return false; }
    @Override public boolean canBeLeashed() { return false; }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) { return InteractionResult.PASS; }
}
