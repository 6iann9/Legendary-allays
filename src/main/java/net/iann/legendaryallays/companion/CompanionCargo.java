package net.iann.legendaryallays.companion;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;

/** All item transfers retain any remainder if another mod refuses an item spawn. */
public final class CompanionCargo {
    public static final String EXCLUDED = "legendaryallays:excluded";

    private CompanionCargo() {}

    public static boolean hasCargo(Allay allay) {
        if (!allay.getInventory().isEmpty()) return true;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!allay.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    public static void dropAll(Allay allay, Vec3 position) {
        transferAll(allay, null, position);
    }

    public static void deliver(Allay allay, ServerPlayer owner) {
        // The wrapper preserves overflow even in creative mode; Inventory.add can delete it.
        transferAll(allay, new PlayerMainInvWrapper(owner.getInventory()), owner.position().add(0, 0.5, 0));
    }

    private static void transferAll(Allay allay, PlayerMainInvWrapper destination, Vec3 position) {
        for (int i = 0; i < allay.getInventory().getContainerSize(); i++) {
            allay.getInventory().setItem(i, transfer(allay, allay.getInventory().getItem(i), destination, position));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            allay.setItemSlot(slot, transfer(allay, allay.getItemBySlot(slot), destination, position));
        }
    }

    private static ItemStack transfer(Allay allay, ItemStack stack, PlayerMainInvWrapper destination, Vec3 position) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remainder = destination == null ? stack.copy()
                : ItemHandlerHelper.insertItemStacked(destination, stack.copy(), false);
        if (remainder.isEmpty()) return ItemStack.EMPTY;
        ItemEntity item = new ItemEntity(allay.level(), position.x, position.y, position.z, remainder.copy());
        item.setDefaultPickUpDelay();
        item.getPersistentData().putBoolean(EXCLUDED, true);
        return allay.level().addFreshEntity(item) ? ItemStack.EMPTY : remainder;
    }
}
