package net.iann.legendaryallays.mixin;

import net.iann.legendaryallays.companion.DropCollection;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep queued loot separate from other players' loot and intentional overflow drops. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void legendaryallays$preserveDropOwnership(ItemEntity other, CallbackInfo callback) {
        if (!DropCollection.mayMerge((ItemEntity) (Object) this, other)) callback.cancel();
    }
}
